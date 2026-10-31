import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/** What we publish. The id lets consumers drop the duplicates a replay can produce. */
record Event(String id, String key, String payload) {}

/** The real broker call, e.g. kafkaProducer.send(record).get(timeout) — it must throw on failure. */
interface Broker { void send(Event e) throws Exception; }

/** Ordered storage for events that could not be published yet. */
interface Spool {
    void append(Event e) throws IOException;
    Event peek();                          // oldest event, or null
    void removeHead() throws IOException;  // call only after the broker acknowledged peek()
    int size();
    List<Event> pending();
}

/**
 * Durable spool: an append-only log of events plus a separate "how many are done" offset.
 * Survives a process restart (put it on a persistent volume, not the container's scratch disk).
 * A crash between the broker's ack and removeHead() re-sends that event: at-least-once.
 */
final class FileSpool implements Spool {
    private final Path log, offsetFile;
    private final Deque<Event> queue = new ArrayDeque<>();
    private long done;                     // events already published, counted from the start of the log

    FileSpool(Path dir) throws IOException {
        Files.createDirectories(dir);
        log = dir.resolve("spool.log");
        offsetFile = dir.resolve("spool.offset");
        done = Files.exists(offsetFile) ? Long.parseLong(Files.readString(offsetFile).trim()) : 0;
        if (Files.exists(log)) {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            for (String line : lines.subList((int) Math.min(done, lines.size()), lines.size())) {
                String[] f = line.split("\t", 3);
                queue.add(new Event(f[0], f[1], f[2]));
            }
        }
    }

    public void append(Event e) throws IOException {
        // One line per event; production code would also fsync (FileChannel.force) per batch.
        String line = e.id() + "\t" + e.key() + "\t" + e.payload().replace('\n', ' ') + "\n";
        Files.writeString(log, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        queue.add(e);
    }

    public Event peek() { return queue.peek(); }

    public void removeHead() throws IOException {
        queue.poll();
        done++;
        if (queue.isEmpty()) {
            // Fully drained: reset the offset *first*, then drop the log. A crash in between only
            // replays old events (duplicates); the reverse order could skip new ones (loss).
            writeOffset(0);
            Files.deleteIfExists(log);
            done = 0;
        } else {
            writeOffset(done);
        }
    }

    private void writeOffset(long value) throws IOException {
        Path tmp = offsetFile.resolveSibling("spool.offset.tmp");
        Files.writeString(tmp, Long.toString(value));
        Files.move(tmp, offsetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public int size() { return queue.size(); }
    public List<Event> pending() { return List.copyOf(queue); }
}

/** Minimal breaker: opens after N consecutive failures, lets traffic try again after a cool-down. */
final class Breaker {
    enum State { CLOSED, OPEN, HALF_OPEN }
    private final int threshold; private final long coolDownMs; private final LongSupplier clock;
    private int failures; private long openedAt; private State state = State.CLOSED;

    Breaker(int threshold, long coolDownMs, LongSupplier clock) {
        this.threshold = threshold; this.coolDownMs = coolDownMs; this.clock = clock;
    }
    synchronized boolean allow() {
        if (state == State.OPEN && clock.getAsLong() - openedAt >= coolDownMs) state = State.HALF_OPEN;
        return state != State.OPEN;
    }
    synchronized void success() { failures = 0; state = State.CLOSED; }
    synchronized void failure() {
        if (state == State.HALF_OPEN || ++failures >= threshold) { state = State.OPEN; openedAt = clock.getAsLong(); }
    }
    synchronized State state() { allow(); return state; }
}

/**
 * Publishes to the broker when it is healthy; otherwise spools to durable storage and replays later.
 * - Live traffic never waits on a broken broker: the breaker makes the decision instantly.
 * - Per-key order is kept: while a key has spooled events, new events for that key queue behind them.
 *   (As with Kafka itself, a caller must publish same-key events one after another.)
 * - Replay is rate-limited so the recovering broker and the consumers are not flooded.
 */
final class ResilientPublisher {
    enum Result { SENT, SPOOLED }
    private final Broker broker; private final Spool spool; private final Breaker breaker; private final LongSupplier clock;
    private final Map<String, Integer> spooledPerKey = new HashMap<>();
    private final double drainPerSecond; private double tokens; private long lastRefill;

    ResilientPublisher(Broker broker, Spool spool, Breaker breaker, double drainPerSecond, LongSupplier clock) {
        this.broker = broker; this.spool = spool; this.breaker = breaker; this.clock = clock;
        this.drainPerSecond = drainPerSecond; this.lastRefill = clock.getAsLong();
        for (Event e : spool.pending()) spooledPerKey.merge(e.key(), 1, Integer::sum); // after a restart
    }

    Result publish(Event e) throws IOException {
        synchronized (this) {
            if (spooledPerKey.containsKey(e.key()) || !breaker.allow()) return spoolLocked(e);
        }
        try {
            broker.send(e);
            breaker.success();
            return Result.SENT;
        } catch (Exception failed) {
            breaker.failure();
            synchronized (this) { return spoolLocked(e); }
        }
    }

    private Result spoolLocked(Event e) throws IOException {
        spool.append(e);
        spooledPerKey.merge(e.key(), 1, Integer::sum);
        return Result.SPOOLED;
    }

    /** Run on a schedule (e.g. every 100 ms) from one background thread. Returns how many were replayed. */
    synchronized int drainOnce() throws IOException {
        long now = clock.getAsLong();
        tokens = Math.min(drainPerSecond, tokens + (now - lastRefill) / 1000.0 * drainPerSecond);
        lastRefill = now;
        int sent = 0;
        while (tokens >= 1 && spool.size() > 0 && breaker.allow()) {
            Event e = spool.peek();
            try {
                broker.send(e);
            } catch (Exception failed) {
                breaker.failure();         // stop; the breaker decides when to try again
                break;
            }
            breaker.success();
            spool.removeHead();
            spooledPerKey.computeIfPresent(e.key(), (k, n) -> n == 1 ? null : n - 1);
            tokens--;
            sent++;
        }
        return sent;
    }

    synchronized int backlog() { return spool.size(); }
}

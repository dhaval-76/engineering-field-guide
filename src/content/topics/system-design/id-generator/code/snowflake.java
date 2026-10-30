public final class Snowflake {
    private static final long EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final int WORKER_BITS = 10, SEQUENCE_BITS = 12;
    private static final long MAX_WORKER = (1L << WORKER_BITS) - 1;      // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;  // 4095
    private static final long MAX_BACKWARDS_MS = 10;

    private final long worker;
    private long lastMs = -1;
    private long sequence;

    public Snowflake(long worker) {
        if (worker < 0 || worker > MAX_WORKER) throw new IllegalArgumentException("worker 0–1023");
        this.worker = worker;
    }

    public synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now < lastMs) {                                     // clock moved backwards
            if (lastMs - now > MAX_BACKWARDS_MS)
                throw new IllegalStateException("clock moved back " + (lastMs - now) + " ms; refusing");
            now = waitUntilAfter(lastMs - 1);                   // small skew: wait it out
        }
        if (now == lastMs) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) now = waitUntilAfter(lastMs);    // 4,096 used this ms
        } else {
            sequence = 0;
        }
        lastMs = now;
        return ((now - EPOCH) << (WORKER_BITS + SEQUENCE_BITS)) | (worker << SEQUENCE_BITS) | sequence;
    }

    private static long waitUntilAfter(long ms) {
        long now;
        while ((now = System.currentTimeMillis()) <= ms) Thread.onSpinWait();
        return now;
    }

    public static Instant timeOf(long id) { return Instant.ofEpochMilli((id >>> 22) + EPOCH); }
    public static long workerOf(long id)  { return (id >>> SEQUENCE_BITS) & MAX_WORKER; }
    public static long sequenceOf(long id){ return id & MAX_SEQUENCE; }
}
// Worker id from a StatefulSet pod name, e.g. "ids-7" → 7:
// long worker = Long.parseLong(System.getenv("HOSTNAME").replaceAll(".*-", ""));

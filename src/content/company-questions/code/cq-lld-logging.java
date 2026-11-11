enum Level {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

record LogEvent(Level level, String logger, String message, long timeMillis, String thread) {}

interface Formatter {
    String format(LogEvent e);
}

interface Appender {
    void append(LogEvent e);
} // console, file, remote: one interface

class PatternFormatter implements Formatter {
    public String format(LogEvent e) {
        return e.level() + " [" + e.thread() + "] " + e.logger() + " - " + e.message();
    }
}

class ListAppender implements Appender { // stands in for console or file
    final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private final Formatter formatter;

    ListAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    public void append(LogEvent e) {
        lines.add(formatter.format(e));
    }
}

/** Decorator: callers hand off to a bounded queue and return; one thread writes. */
class AsyncAppender implements Appender {
    private final BlockingQueue<LogEvent> queue = new ArrayBlockingQueue<>(10_000);
    final AtomicLong dropped = new AtomicLong();
    private final Thread writer;

    AsyncAppender(Appender target) {
        writer =
                Thread.ofPlatform()
                        .daemon()
                        .start(
                                () -> {
                                    try {
                                        while (true) target.append(queue.take());
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                });
    }

    public void append(LogEvent e) {
        if (!queue.offer(e)) dropped.incrementAndGet(); // full: drop rather than block callers
    }

    void flush() throws InterruptedException {
        while (!queue.isEmpty()) Thread.sleep(1);
        Thread.sleep(5);
    }
}

class Logger {
    private final String name;
    private volatile Level threshold;
    private final List<Appender> appenders = new CopyOnWriteArrayList<>();

    Logger(String name, Level threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    void addAppender(Appender a) {
        appenders.add(a);
    }

    void setLevel(Level level) {
        threshold = level;
    } // change at runtime, no restart

    void log(Level level, Supplier<String> message) { // Supplier: no string work if filtered
        if (level.compareTo(threshold) < 0) return;
        LogEvent e =
                new LogEvent(
                        level,
                        name,
                        message.get(),
                        System.currentTimeMillis(),
                        Thread.currentThread().getName());
        for (Appender a : appenders) a.append(e);
    }

    void info(String m) {
        log(Level.INFO, () -> m);
    }

    void debug(Supplier<String> m) {
        log(Level.DEBUG, m);
    }

    void error(String m) {
        log(Level.ERROR, () -> m);
    }
}

/** One Logger per name, created once and shared: the registry is the only singleton. */
final class LoggerFactory {
    private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

    private LoggerFactory() {}

    static Logger get(String name) {
        return LOGGERS.computeIfAbsent(name, n -> new Logger(n, Level.INFO));
    }
}

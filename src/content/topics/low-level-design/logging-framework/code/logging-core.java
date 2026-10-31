enum Level { DEBUG, INFO, WARN, ERROR }

record LogEvent(Instant time, Level level, String logger, String thread, String message, Map<String, String> context) {}

interface Formatter { String format(LogEvent e); }
interface Appender extends AutoCloseable { void append(LogEvent e); @Override default void close() {} }

/** "2026-10-31T09:15:02Z INFO  [main] com.shop.orders - placed order {orderId=42}" */
class PatternFormatter implements Formatter {
    public String format(LogEvent e) {
        return "%s %-5s [%s] %s - %s%s".formatted(e.time(), e.level(), e.thread(), e.logger(), e.message(),
                e.context().isEmpty() ? "" : " " + e.context());
    }
}

class ConsoleAppender implements Appender {
    private final Formatter formatter;
    ConsoleAppender(Formatter formatter) { this.formatter = formatter; }
    public synchronized void append(LogEvent e) {
        (e.level().compareTo(Level.WARN) >= 0 ? System.err : System.out).println(formatter.format(e));
    }
}

/** Loggers form a tree by dotted name; a logger with no level inherits its nearest ancestor's. */
class Logger {
    private final String name;
    private final Logger parent;
    private volatile Level level;                              // null = inherit
    private final List<Appender> appenders = new CopyOnWriteArrayList<>();
    // also send to ancestors' appenders
    private volatile boolean additive = true;
    // MDC
    static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(LinkedHashMap::new);

    Logger(String name, Logger parent) { this.name = name; this.parent = parent; }

    Level effectiveLevel() {
        for (Logger l = this; l != null; l = l.parent) if (l.level != null) return l.level;
        return Level.INFO;
    }

    boolean isEnabled(Level l) { return l.compareTo(effectiveLevel()) >= 0; }

    // supplier: no string building when disabled
    void log(Level l, Supplier<String> message) {
        if (!isEnabled(l)) return;
        LogEvent e = new LogEvent(Instant.now(), l, name, Thread.currentThread().getName(),
                message.get(), Map.copyOf(CONTEXT.get()));
        for (Logger x = this; x != null; x = x.parent) {
            for (Appender a : x.appenders) a.append(e);
            if (!x.additive) break;
        }
    }

    void debug(Supplier<String> m) { log(Level.DEBUG, m); }
    void info(String m)  { log(Level.INFO, () -> m); }
    void warn(String m)  { log(Level.WARN, () -> m); }
    void error(String m) { log(Level.ERROR, () -> m); }

    void setLevel(Level level) { this.level = level; }
    void addAppender(Appender a) { appenders.add(a); }
    void setAdditive(boolean additive) { this.additive = additive; }
}

/** One registry per process: the root logger plus every named logger, created on demand. */
final class LoggerFactory {
    private static final Logger ROOT = new Logger("root", null);
    private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

    static Logger root() { return ROOT; }

    static Logger getLogger(String name) {
        return LOGGERS.computeIfAbsent(name, n -> {
            int dot = n.lastIndexOf('.');
            // com.shop → com
            Logger parent = dot < 0 ? ROOT : getLogger(n.substring(0, dot));
            return new Logger(n, parent);
        });
    }
}

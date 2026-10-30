// ✗ One interface for every destination.
interface Logger { void log(String m); void logToFile(String m); void logToDb(String m); void logToSplunk(String m); }

// ✓ One small contract; destinations are separate implementations (or composed sinks).
interface LogSink { void write(LogEvent e); }
class ConsoleSink implements LogSink { public void write(LogEvent e) { System.out.println(e); } }
class FileSink    implements LogSink { public void write(LogEvent e) { /* append */ } }
class SplunkSink  implements LogSink { public void write(LogEvent e) { /* HTTP */ } }

class Logger {
    private final List<LogSink> sinks;
    Logger(List<LogSink> sinks) { this.sinks = sinks; }
    void info(String msg) { var e = LogEvent.info(msg); sinks.forEach(s -> s.write(e)); }
}

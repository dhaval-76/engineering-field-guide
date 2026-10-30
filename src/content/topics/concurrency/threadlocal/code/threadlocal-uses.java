// 1. Per-thread instances of a non-thread-safe object (pre-java.time example)
private static final ThreadLocal<SimpleDateFormat> FORMAT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
// Better today: DateTimeFormatter is immutable and thread-safe — no ThreadLocal needed.

// 2. Logging context: SLF4J's MDC is a ThreadLocal map that log patterns can print (%X{traceId})
MDC.put("traceId", traceId);
try { handle(request); } finally { MDC.remove("traceId"); }

// 3. Frameworks: Spring's TransactionSynchronizationManager and SecurityContextHolder
//    keep the current transaction / authenticated user in ThreadLocals.

// Passing context to another thread: capture, then restore inside the task
Map<String, String> mdc = MDC.getCopyOfContextMap();
executor.submit(() -> {
    MDC.setContextMap(mdc);
    try { sendEmail(); } finally { MDC.clear(); }
});

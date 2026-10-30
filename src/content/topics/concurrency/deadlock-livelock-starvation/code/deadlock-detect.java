// In production: jstack <pid>, `jcmd <pid> Thread.print`, or GET /actuator/threaddump.
// The dump ends with "Found one Java-level deadlock:" and names each thread, the lock it holds and the one it wants.

// Automated detection, e.g. in a health check:
ThreadMXBean threads = ManagementFactory.getThreadMXBean();
long[] ids = threads.findDeadlockedThreads();          // monitors AND java.util.concurrent locks
if (ids != null) {
    for (ThreadInfo info : threads.getThreadInfo(ids, true, true)) {
        log.error("deadlocked: {} waiting for {} held by {}",
                  info.getThreadName(), info.getLockName(), info.getLockOwnerName());
    }
    health.markDown("deadlock");                       // let the orchestrator restart the instance
}

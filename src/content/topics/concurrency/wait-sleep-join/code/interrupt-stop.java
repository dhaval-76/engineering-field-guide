// ✗ Thread.stop() — unsafe and, since Java 20, throws UnsupportedOperationException. It
// killed the thread at an arbitrary point, releasing its locks with objects half-updated.

// ✓ Cooperative cancellation: ask with interrupt(), and let the thread stop at a safe
// point.
class Poller implements Runnable {
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
                // blocking calls throw InterruptedException
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                // restore the flag so callers can see it
                Thread.currentThread().interrupt();
                break;                                  // clean up and exit
            }
        }
        closeConnections();
    }
}

Thread t = Thread.ofPlatform().name("poller").start(new Poller());
t.interrupt();          // request stop
t.join(2_000);          // give it time to finish cleanly

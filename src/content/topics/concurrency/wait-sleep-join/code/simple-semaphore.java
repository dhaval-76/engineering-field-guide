class SimpleSemaphore {
    private int permits;

    SimpleSemaphore(int permits) { this.permits = permits; }

    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait();
        }
        permits--;
    }

    public synchronized void release() {
        permits++;
        notifyAll();      // waiters re-check `permits`; only one can take the new permit
    }
}
// In real code use java.util.concurrent.Semaphore (fairness option, tryAcquire with
// timeout).

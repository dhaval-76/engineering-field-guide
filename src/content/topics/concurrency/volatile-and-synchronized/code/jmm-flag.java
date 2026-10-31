// ✗ Without volatile this loop may never end: the JIT is allowed to read `running` once
//   and keep it in a register, because nothing tells it another thread will change it.
class Worker implements Runnable {
    private boolean running = true;
    public void run() { while (running) { doWork(); } }
    public void stop() { running = false; }
}

// ✓ volatile: every read sees the latest write; no caching or reordering across it
class Worker implements Runnable {
    private volatile boolean running = true;
    public void run() { while (running) { doWork(); } }
    public void stop() { running = false; }
}
// Even better for stopping threads: interruption (thread.interrupt() /
// Thread.interrupted()), which also wakes threads blocked in sleep/wait/queue.take().

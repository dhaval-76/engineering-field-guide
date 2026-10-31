// sleep: pause this thread for a time. Keeps any locks it holds.
// Java 19+ overload; sleep(200) works everywhere
Thread.sleep(Duration.ofMillis(200));

// join: wait for another thread to finish (optionally with a timeout).
Thread worker = Thread.ofPlatform().name("report-builder").start(this::buildReport);
// returns early if the worker ends sooner
worker.join(Duration.ofSeconds(5));
if (worker.isAlive()) log.warn("report still running");

// wait/notify: wait for a CONDITION another thread will make true. Must hold the object's
// monitor.
synchronized (lock) {
    // always a loop: spurious wakeups and stolen conditions
    while (!ready) {
        // releases `lock`, sleeps, re-acquires before returning
        lock.wait();
    }
    use(data);
}
// …in another thread
synchronized (lock) {
    data = load();
    ready = true;
    lock.notifyAll();                            // wake waiters; they re-check `ready`
}

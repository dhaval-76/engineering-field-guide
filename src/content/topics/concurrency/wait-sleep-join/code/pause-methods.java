// sleep: pause this thread for a time. Keeps any locks it holds.
Thread.sleep(Duration.ofMillis(200));            // Java 19+ overload; sleep(200) works everywhere

// join: wait for another thread to finish (optionally with a timeout).
Thread worker = Thread.ofPlatform().name("report-builder").start(this::buildReport);
worker.join(Duration.ofSeconds(5));              // returns early if the worker ends sooner
if (worker.isAlive()) log.warn("report still running");

// wait/notify: wait for a CONDITION another thread will make true. Must hold the object's monitor.
synchronized (lock) {
    while (!ready) {                             // always a loop: spurious wakeups and stolen conditions
        lock.wait();                             // releases `lock`, sleeps, re-acquires before returning
    }
    use(data);
}
// …in another thread
synchronized (lock) {
    data = load();
    ready = true;
    lock.notifyAll();                            // wake waiters; they re-check `ready`
}

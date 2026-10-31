// A thread runs a Runnable. Prefer executors over raw threads in application code.
Runnable task = () -> System.out.println("hello from " + Thread.currentThread().getName());
Thread t = new Thread(task, "worker-1");
// start() creates the new thread; calling run() directly would run on the CURRENT thread
t.start();
t.join();           // wait for it to finish

// Platform thread vs virtual thread (Java 21)
// backed 1:1 by an OS thread (~1 MB stack)
Thread.ofPlatform().name("io-", 0).start(task);
// scheduled by the JVM; cheap enough to create millions
Thread.ofVirtual().start(task);

// Lifecycle: NEW → RUNNABLE → (BLOCKED | WAITING | TIMED_WAITING) → RUNNABLE → TERMINATED

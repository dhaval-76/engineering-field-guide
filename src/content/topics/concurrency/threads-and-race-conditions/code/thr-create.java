// A thread runs a Runnable. Prefer executors over raw threads in application code.
Thread t = new Thread(() -> System.out.println("hello from " + Thread.currentThread().getName()), "worker-1");
t.start();          // start() creates the new thread; calling run() directly would run on the CURRENT thread
t.join();           // wait for it to finish

// Platform thread vs virtual thread (Java 21)
Thread.ofPlatform().name("io-", 0).start(task);   // backed 1:1 by an OS thread (~1 MB stack)
Thread.ofVirtual().start(task);                   // scheduled by the JVM; cheap enough to create millions

// Lifecycle: NEW → RUNNABLE → (BLOCKED | WAITING | TIMED_WAITING) → RUNNABLE → TERMINATED

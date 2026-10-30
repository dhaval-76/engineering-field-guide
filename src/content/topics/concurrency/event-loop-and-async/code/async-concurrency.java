// Java's answer to the same problem: virtual threads (Java 21+).
// Write plain blocking code; the JVM parks the virtual thread during I/O and frees the carrier thread.
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    Semaphore limit = new Semaphore(10);                    // still protect downstreams
    List<Future<String>> pages = urls.stream()
        .map(url -> exec.submit(() -> {
            limit.acquire();
            try { return http.send(get(url), ofString()).body(); }
            finally { limit.release(); }
        }))
        .toList();
    for (Future<String> p : pages) handle(p.get());
}
// Reactive stacks (WebFlux, Netty) are event loops like Node's: never block their threads.

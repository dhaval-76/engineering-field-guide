// Virtual threads (21): cheap threads managed by the JVM. Blocking code, massive
// concurrency.
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Profile>> futures = userIds.stream()
            // plain blocking HTTP call
            .map(id -> executor.submit(() -> profileClient.fetch(id)))
            .toList();
    for (Future<Profile> f : futures) render(f.get());
}   // 10,000 concurrent blocking calls without 10,000 OS threads

// Spring Boot 3.2+: spring.threads.virtual.enabled=true → request handling on virtual
// threads. They help I/O-bound work (waiting on network/DB), not CPU-bound work.

// Reproduce exhaustion: pool of 2, ten requests that each hold a connection for 2 s, timeout 3 s.
@SpringBootTest(properties = {
    "spring.datasource.hikari.maximum-pool-size=2",
    "spring.datasource.hikari.connection-timeout=3000" })
class PoolExhaustionTest {
    @Autowired DataSource ds;

    @Test
    void requestsQueueThenTimeOut() throws Exception {
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> results = IntStream.range(0, 10)
                .mapToObj(i -> exec.submit(() -> {
                    long start = System.nanoTime();
                    try (Connection c = ds.getConnection()) {
                        c.createStatement().execute("SELECT pg_sleep(2)");
                        return "ok after " + Duration.ofNanos(System.nanoTime() - start).toMillis() + " ms";
                    } catch (SQLTransientConnectionException e) {
                        return "timed out waiting for a connection";
                    }
                }))
                .toList();
            for (Future<String> r : results) System.out.println(r.get());
        }
        // Expect: 2 finish after ~2 s, 2 after ~4 s (waited ~2 s), the rest time out at 3 s of waiting.
    }
}

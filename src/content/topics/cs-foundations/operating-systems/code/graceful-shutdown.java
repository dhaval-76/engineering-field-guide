public class Worker {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        // Runs on SIGTERM and SIGINT (and normal exit). Nothing runs on SIGKILL.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("SIGTERM: draining");
            pool.shutdown(); // accept no new work
            try {
                if (!pool.awaitTermination(20, TimeUnit.SECONDS)) pool.shutdownNow();
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
            System.out.println("drained");
        }));
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                sleep(500); // a request in flight
                System.out.println("job done");
            });
        }
        Thread.currentThread().join(); // serve until a signal arrives
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

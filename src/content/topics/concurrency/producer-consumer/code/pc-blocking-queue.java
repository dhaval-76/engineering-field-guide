// bounded: producers slow down when full
BlockingQueue<Order> queue = new ArrayBlockingQueue<>(1_000);

// Producer — e.g. an HTTP handler accepting orders
// wait briefly, then give up
boolean accepted = queue.offer(order, 50, MILLISECONDS);
// backpressure → 503 / retry later
if (!accepted) throw new ServiceOverloadedException();

// Consumers — a fixed number of workers draining the queue
ExecutorService workers = Executors.newFixedThreadPool(4);
for (int i = 0; i < 4; i++) {
    workers.submit(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order o = queue.take();                          // blocks while empty
                fulfil(o);
            } catch (InterruptedException e) {
                // shutdownNow() lands here
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // one bad item mustn't kill the worker
                log.error("order failed", e);
            }
        }
    });
}

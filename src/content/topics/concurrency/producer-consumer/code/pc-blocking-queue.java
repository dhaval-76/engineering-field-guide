BlockingQueue<Order> queue = new ArrayBlockingQueue<>(1_000);   // bounded: producers slow down when full

// Producer — e.g. an HTTP handler accepting orders
boolean accepted = queue.offer(order, 50, MILLISECONDS);         // wait briefly, then give up
if (!accepted) throw new ServiceOverloadedException();          // backpressure → 503 / retry later

// Consumers — a fixed number of workers draining the queue
ExecutorService workers = Executors.newFixedThreadPool(4);
for (int i = 0; i < 4; i++) {
    workers.submit(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order o = queue.take();                          // blocks while empty
                fulfil(o);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();              // shutdownNow() lands here
            } catch (Exception e) {
                log.error("order failed", e);                    // one bad item mustn't kill the worker
            }
        }
    });
}

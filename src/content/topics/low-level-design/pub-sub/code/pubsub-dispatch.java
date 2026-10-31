/** Push delivery for one group: poll, hand to the handler, retry with backoff, dead-letter, then commit. */
class Dispatcher implements Runnable {
    private final Broker broker;
    private final String topic, group;
    private final Consumer<Message> handler;
    private final int maxAttempts;
    private volatile boolean running = true;

    Dispatcher(Broker broker, String topic, String group, Consumer<Message> handler, int maxAttempts) {
        this.broker = broker; this.topic = topic; this.group = group;
        this.handler = handler; this.maxAttempts = maxAttempts;
        broker.subscribe(topic, group, false);
    }

    @Override public void run() {
        while (running) {
            List<Message> batch = broker.poll(topic, group, 100);
            // or block until publish notifies
            if (batch.isEmpty()) { sleepQuietly(50); continue; }
            for (Message m : batch) {
                deliver(m);
                // commit after handling: at-least-once
                broker.commit(topic, group, m.offset() + 1);
            }
        }
    }

    private void deliver(Message m) {
        for (int attempt = 1; ; attempt++) {
            try { handler.accept(m); return; }
            catch (RuntimeException e) {
                // poison message: don't block the topic
                if (attempt == maxAttempts) {
                    broker.publish(topic + ".dlq", m.key(), m.payload());
                    return;
                }
                // exponential backoff
                sleepQuietly(100L << attempt);
            }
        }
    }

    void stop() { running = false; }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

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
            if (batch.isEmpty()) { sleepQuietly(50); continue; }      // or block until publish notifies
            for (Message m : batch) {
                deliver(m);
                broker.commit(topic, group, m.offset() + 1);          // commit after handling: at-least-once
            }
        }
    }

    private void deliver(Message m) {
        for (int attempt = 1; ; attempt++) {
            try { handler.accept(m); return; }
            catch (RuntimeException e) {
                if (attempt == maxAttempts) {                           // poison message: don't block the topic
                    broker.publish(topic + ".dlq", m.key(), m.payload());
                    return;
                }
                sleepQuietly(100L << attempt);                          // exponential backoff
            }
        }
    }

    void stop() { running = false; }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

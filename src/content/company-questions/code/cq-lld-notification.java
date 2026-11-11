enum Channel {
    EMAIL,
    SMS,
    PUSH
}

record User(String id, String email, String phone, Set<Channel> channels) {}

class DeliveryException extends Exception {
    final boolean retryable;

    DeliveryException(String msg, boolean retryable) {
        super(msg);
        this.retryable = retryable;
    }
}

/** Strategy: one interface, one class per channel. */
interface NotificationSender {
    void send(User user, String message) throws DeliveryException;
}

/** Factory: maps a channel to its sender, so adding WhatsApp is a new class plus one entry. */
class SenderFactory {
    private final Map<Channel, NotificationSender> senders = new EnumMap<>(Channel.class);

    void register(Channel c, NotificationSender s) {
        senders.put(c, s);
    }

    NotificationSender forChannel(Channel c) {
        NotificationSender s = senders.get(c);
        if (s == null) throw new IllegalArgumentException("no sender for " + c);
        return s;
    }
}

record Result(Channel channel, boolean delivered, int attempts, String error) {}

class NotificationService { // a single shared instance, created once (DI, not a static)
    private final SenderFactory factory;
    private final int maxAttempts;

    NotificationService(SenderFactory factory, int maxAttempts) {
        this.factory = factory;
        this.maxAttempts = maxAttempts;
    }

    List<Result> notify(User user, String message) {
        List<Result> results = new ArrayList<>();
        for (Channel c : user.channels()) { // one channel failing doesn't stop the others
            NotificationSender sender = factory.forChannel(c);
            int attempt = 0;
            String error = null;
            boolean ok = false;
            while (!ok && attempt < maxAttempts) {
                attempt++;
                try {
                    sender.send(user, message);
                    ok = true;
                } catch (DeliveryException e) {
                    error = e.getMessage();
                    if (!e.retryable) break; // a bad number won't get better
                    backoff(attempt);
                }
            }
            results.add(new Result(c, ok, attempt, ok ? null : error));
        }
        return results;
    }

    void backoff(int attempt) { // exponential: 10 ms, 20 ms, 40 ms…
        try {
            Thread.sleep(10L << (attempt - 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

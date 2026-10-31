enum Channel { PUSH, EMAIL, SMS, IN_APP }

interface ChannelSender { Channel channel(); void send(User u, Notification n) throws DeliveryException; }

record Preferences(List<Channel> broadcast, List<Channel> fallbackOrder, LocalTime quietFrom, LocalTime quietTo) {}

final class NotificationService {
    private final Map<Channel, ChannelSender> senders;
    private final PreferenceStore prefs;
    private final DeliveryLog log;                                // idempotency + audit

    NotificationService(List<ChannelSender> all, PreferenceStore prefs, DeliveryLog log) {
        this.senders = all.stream().collect(Collectors.toUnmodifiableMap(ChannelSender::channel, s -> s));
        this.prefs = prefs; this.log = log;
    }

    /** Broadcast: every preferred channel independently (e.g. in-app + push for a reminder). */
    void broadcast(User u, Notification n) {
        for (Channel c : prefs.of(u).broadcast()) deliverOnce(u, n, c);
    }

    /** Fallback chain: try channels in order until one succeeds (e.g. OTP: push → SMS → email). */
    boolean deliverWithFallback(User u, Notification n) {
        for (Channel c : prefs.of(u).fallbackOrder()) {
            if (deliverOnce(u, n, c)) return true;
        }
        return false;
    }

    private boolean deliverOnce(User u, Notification n, Channel c) {
        // already sent on this channel: idempotent
        if (!log.claim(n.id(), c)) return true;
        try {
            senders.get(c).send(u, n);
            log.markSent(n.id(), c);
            return true;
        } catch (DeliveryException e) {
            log.markFailed(n.id(), c, e.getMessage());
            return false;
        }
    }
}

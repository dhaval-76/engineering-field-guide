class DeliveryError(Exception):
    pass

class NotificationService:
    """senders: {channel: sender with .send(user, n)}; log gives idempotency + audit."""

    def __init__(self, senders: dict, prefs, log):
        self.senders, self.prefs, self.log = senders, prefs, log

    def broadcast(self, user, n) -> None:
        """Every preferred channel independently (e.g. in-app + push for a reminder)."""
        for c in self.prefs.of(user).broadcast:
            self._deliver_once(user, n, c)

    def deliver_with_fallback(self, user, n) -> bool:
        """Try channels in order until one succeeds (e.g. OTP: push → SMS → email)."""
        return any(self._deliver_once(user, n, c) for c in self.prefs.of(user).fallback_order)

    def _deliver_once(self, user, n, channel) -> bool:
        if not self.log.claim(n.id, channel):
            return True                                  # already sent on this channel: idempotent
        try:
            self.senders[channel].send(user, n)
            self.log.mark_sent(n.id, channel)
            return True
        except DeliveryError as e:
            self.log.mark_failed(n.id, channel, str(e))
            return False

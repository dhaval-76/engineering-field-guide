// senders: Map(channel -> { send(user, notification) → Promise }); log gives idempotency + audit.
export class NotificationService {
  constructor({ senders, prefs, log }) {
    Object.assign(this, { senders, prefs, log });
  }

  // Broadcast: every preferred channel independently (e.g. in-app + push for a reminder).
  async broadcast(user, n) {
    for (const c of this.prefs.of(user).broadcast) await this.#deliverOnce(user, n, c);
  }

  // Fallback chain: try channels in order until one succeeds (e.g. OTP: push → SMS → email).
  async deliverWithFallback(user, n) {
    for (const c of this.prefs.of(user).fallbackOrder) if (await this.#deliverOnce(user, n, c)) return true;
    return false;
  }

  async #deliverOnce(user, n, channel) {
    if (!(await this.log.claim(n.id, channel))) return true; // already sent on this channel: idempotent
    try {
      await this.senders.get(channel).send(user, n);
      await this.log.markSent(n.id, channel);
      return true;
    } catch (e) {
      await this.log.markFailed(n.id, channel, e.message);
      return false;
    }
  }
}

// Decides where a notification goes: dedup, preferences, quiet hours (user's time zone),
// frequency caps.
export class NotificationRouter {
  #seen = new Map(); // dedupKey -> epoch seconds   (Redis SET NX EX in production)
  #marketing = new Map(); // userId -> [send times]

  constructor(marketingPerDay) {
    this.marketingPerDay = marketingPerDay;
  }

  route(n, prefs, nowMs) {
    const t = Math.floor(nowMs / 1000);
    const seen = this.#seen.get(n.dedupKey);
    if (seen !== undefined && t - seen < 86400)
      return { sendNow: [], deferred: [], reason: 'duplicate' };
    this.#seen.set(n.dedupKey, t);

    const critical = n.category === 'SECURITY';
    if (!critical && prefs.mutedCategories.includes(n.category))
      return { sendNow: [], deferred: [], reason: 'muted by user' };

    if (n.category === 'MARKETING') {
      const sent = (this.#marketing.get(n.userId) ?? []).filter((s) => t - s < 86400);
      if (sent.length >= this.marketingPerDay)
        return { sendNow: [], deferred: [], reason: 'frequency cap' };
      this.#marketing.set(n.userId, [...sent, t]);
    }

    const allowed = n.preferredChannels.filter((c) => critical || !prefs.optedOut.includes(c));
    if (!allowed.length) return { sendNow: [], deferred: [], reason: 'no allowed channel' };

    const quiet = !critical && inQuietHours(nowMs, prefs);
    const sendNow = allowed.filter((c) => !quiet || c === 'IN_APP');
    const deferred = allowed.filter((c) => quiet && c !== 'IN_APP');
    return { sendNow, deferred, reason: quiet ? 'quiet hours' : 'ok' };
  }
}

function inQuietHours(nowMs, { quietStart, quietEnd, timeZone }) {
  if (!quietStart) return false;
  const hm = new Intl.DateTimeFormat('en-GB', {
    timeZone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(nowMs);
  // "HH:MM" compares as text
  return quietStart < quietEnd
    ? hm >= quietStart && hm < quietEnd
    : hm >= quietStart || hm < quietEnd;
}

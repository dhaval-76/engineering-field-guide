const TRACKING = new Set(['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'gclid', 'fbclid']);

// One canonical form per page (the URL class lower-cases host and drops default ports).
export function normalise(url) {
  const u = new URL(url.trim());
  const params = [...u.searchParams].filter(([k]) => !TRACKING.has(k)).sort(([a, av], [b, bv]) => (a + '=' + av < b + '=' + bv ? -1 : 1));
  u.search = new URLSearchParams(params).toString();
  u.hash = '';
  return u.toString();
}

// Per-host FIFO queues plus "ready" times; a heap in production, a scan here for brevity.
export class PoliteFrontier {
  queues = new Map();
  delay = new Map(); // robots.txt Crawl-delay or adaptive
  nextAllowed = new Map();
  readyAt = new Map(); // host -> time it may next be fetched (only hosts with queued URLs)
  seen = new Set(); // a Bloom filter at web scale

  constructor(defaultDelaySeconds = 1) {
    this.defaultDelay = defaultDelaySeconds;
  }

  add(url, now) {
    const u = normalise(url);
    if (this.seen.has(u)) return false;
    this.seen.add(u);
    const host = new URL(u).hostname;
    if (!this.queues.get(host)?.length) {
      this.queues.set(host, this.queues.get(host) ?? []);
      this.readyAt.set(host, Math.max(now, this.nextAllowed.get(host) ?? now));
    }
    this.queues.get(host).push(u);
    return true;
  }

  next(now) {
    let best = null;
    for (const [host, at] of this.readyAt)
      if (at <= now && (best === null || at < this.readyAt.get(best) || (at === this.readyAt.get(best) && host < best))) best = host;
    if (best === null) return null;
    const q = this.queues.get(best);
    const url = q.shift();
    this.nextAllowed.set(best, now + (this.delay.get(best) ?? this.defaultDelay));
    if (q.length) this.readyAt.set(best, this.nextAllowed.get(best));
    else this.readyAt.delete(best);
    return url;
  }
}

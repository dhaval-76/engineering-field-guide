import heapq
from collections import deque
from urllib.parse import urlsplit, urlunsplit, parse_qsl, urlencode

TRACKING = {"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "gclid", "fbclid"}

def normalise(url: str) -> str:
    """One canonical form per page, so the same page isn't crawled under many URLs."""
    p = urlsplit(url.strip())
    scheme, host = p.scheme.lower(), (p.hostname or "").lower()
    port = p.port
    netloc = host if port is None or (scheme, port) in (("http", 80), ("https", 443)) else f"{host}:{port}"
    query = urlencode(sorted((k, v) for k, v in parse_qsl(p.query, keep_blank_values=True) if k not in TRACKING))
    # drop the #fragment
    return urlunsplit((scheme, netloc, p.path or "/", query, ""))

class PoliteFrontier:
    """Per-host FIFO queues plus a heap of (next time this host may be fetched, host)."""

    def __init__(self, default_delay_s: float = 1.0):
        self.default_delay_s = default_delay_s
        self.queues: dict[str, deque[str]] = {}
        # from robots.txt Crawl-delay, or adaptive
        self.delay: dict[str, float] = {}
        self.ready: list[tuple[float, str]] = []      # heap of hosts with queued URLs
        # earliest time each host may be contacted again
        self.next_allowed: dict[str, float] = {}
        self.seen: set[str] = set()                   # a Bloom filter at web scale

    def add(self, url: str, now: float) -> bool:
        u = normalise(url)
        if u in self.seen:
            return False
        self.seen.add(u)
        host = urlsplit(u).hostname
        if host not in self.queues or not self.queues[host]:
            self.queues.setdefault(host, deque())
            heapq.heappush(self.ready, (max(now, self.next_allowed.get(host, now)), host))
        self.queues[host].append(u)
        return True

    def next(self, now: float) -> str | None:
        """The next URL whose host may be contacted now, or None if every host is cooling down."""
        if not self.ready or self.ready[0][0] > now:
            return None
        _, host = heapq.heappop(self.ready)
        url = self.queues[host].popleft()
        self.next_allowed[host] = now + self.delay.get(host, self.default_delay_s)
        if self.queues[host]:
            heapq.heappush(self.ready, (self.next_allowed[host], host))
        return url

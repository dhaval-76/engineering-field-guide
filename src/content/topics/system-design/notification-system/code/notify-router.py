import threading
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime, time
from zoneinfo import ZoneInfo

@dataclass(frozen=True)
class Notification:
    id: str
    user_id: str
    category: str                 # SECURITY | TRANSACTIONAL | SOCIAL | MARKETING
    dedup_key: str
    preferred_channels: tuple[str, ...]

@dataclass(frozen=True)
class Prefs:
    opted_out: frozenset = frozenset()
    muted_categories: frozenset = frozenset()
    quiet_start: time | None = None
    quiet_end: time | None = None
    zone: str = "UTC"

@dataclass(frozen=True)
class Decision:
    send_now: list
    deferred: list
    reason: str

class NotificationRouter:
    """Decides where a notification goes: dedup, preferences, quiet hours, frequency caps."""

    def __init__(self, marketing_per_day: int):
        self.marketing_per_day = marketing_per_day
        self._seen: dict[str, float] = {}                 # Redis SET NX EX in production
        self._marketing: dict[str, deque] = {}
        self._lock = threading.Lock()

    def route(self, n: Notification, p: Prefs, now: datetime) -> Decision:
        t = now.timestamp()
        with self._lock:
            seen = self._seen.get(n.dedup_key)
            if seen is not None and t - seen < 86400:
                return Decision([], [], "duplicate")
            self._seen[n.dedup_key] = t

            critical = n.category == "SECURITY"
            if not critical and n.category in p.muted_categories:
                return Decision([], [], "muted by user")

            if n.category == "MARKETING":
                sent = self._marketing.setdefault(n.user_id, deque())
                while sent and t - sent[0] >= 86400:
                    sent.popleft()
                if len(sent) >= self.marketing_per_day:
                    return Decision([], [], "frequency cap")
                sent.append(t)

        allowed = [c for c in n.preferred_channels if critical or c not in p.opted_out]
        if not allowed:
            return Decision([], [], "no allowed channel")
        quiet = not critical and _in_quiet_hours(now, p)
        send_now = [c for c in allowed if not quiet or c == "IN_APP"]
        deferred = [c for c in allowed if quiet and c != "IN_APP"]
        return Decision(send_now, deferred, "quiet hours" if quiet else "ok")

def _in_quiet_hours(now: datetime, p: Prefs) -> bool:
    if p.quiet_start is None:
        return False
    local = now.astimezone(ZoneInfo(p.zone)).time()
    if p.quiet_start < p.quiet_end:
        return p.quiet_start <= local < p.quiet_end
    return local >= p.quiet_start or local < p.quiet_end      # window crosses midnight

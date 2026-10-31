import os
import threading
import time
from dataclasses import dataclass
from typing import Callable, Optional


@dataclass(frozen=True)
class Event:
    id: str  # lets consumers drop the duplicates a replay can produce
    key: str
    payload: str


# broker: any callable send(event) that raises on failure, e.g. producer.produce(...) +
# flush(timeout)
Broker = Callable[[Event], None]


class FileSpool:
    """Durable spool: an append-only log plus a separate "how many are done" offset.

    Put it on a persistent volume. A crash between the broker's ack and remove_head()
    re-sends that event (at-least-once)."""

    def __init__(self, directory: str):
        os.makedirs(directory, exist_ok=True)
        self.log = os.path.join(directory, "spool.log")
        self.offset_file = os.path.join(directory, "spool.offset")
        self.done = int(open(self.offset_file).read()) if os.path.exists(self.offset_file) else 0
        lines = (
            open(self.log, encoding="utf-8").read().splitlines() if os.path.exists(self.log) else []
        )
        self.queue: list[Event] = []
        for line in lines[self.done :]:
            id_, key, payload = line.split("\t", 2)
            self.queue.append(Event(id_, key, payload))
        self.head = 0

    def append(self, e: Event) -> None:
        with open(self.log, "a", encoding="utf-8") as f:
            f.write(f"{e.id}\t{e.key}\t{e.payload.replace(chr(10), ' ')}\n")
            f.flush()  # production code would also os.fsync(f.fileno()) per batch
        self.queue.append(e)

    def peek(self) -> Optional[Event]:
        return self.queue[self.head] if self.head < len(self.queue) else None

    def remove_head(self) -> None:
        self.head += 1
        self.done += 1
        if self.head == len(self.queue):
            # Reset the offset first, then drop the log: a crash in between replays, never
            # skips.
            self._write_offset(0)
            if os.path.exists(self.log):
                os.remove(self.log)
            self.queue, self.head, self.done = [], 0, 0
        else:
            self._write_offset(self.done)

    def _write_offset(self, value: int) -> None:
        tmp = self.offset_file + ".tmp"
        with open(tmp, "w") as f:
            f.write(str(value))
        os.replace(tmp, self.offset_file)  # atomic replace

    def size(self) -> int:
        return len(self.queue) - self.head

    def pending(self) -> list[Event]:
        return self.queue[self.head :]


class Breaker:
    """Minimal breaker: opens after N consecutive failures, lets traffic try again after a cool-down."""

    def __init__(
        self, threshold: int, cool_down_s: float, clock: Callable[[], float] = time.monotonic
    ):
        self.threshold, self.cool_down_s, self.clock = threshold, cool_down_s, clock
        self.failures, self.opened_at, self.state = 0, 0.0, "CLOSED"
        self._lock = threading.Lock()

    def allow(self) -> bool:
        with self._lock:
            if self.state == "OPEN" and self.clock() - self.opened_at >= self.cool_down_s:
                self.state = "HALF_OPEN"
            return self.state != "OPEN"

    def success(self) -> None:
        with self._lock:
            self.failures, self.state = 0, "CLOSED"

    def failure(self) -> None:
        with self._lock:
            self.failures += 1
            if self.state == "HALF_OPEN" or self.failures >= self.threshold:
                self.state, self.opened_at = "OPEN", self.clock()


class ResilientPublisher:
    """Publishes when the broker is healthy; otherwise spools durably and replays later, rate-limited.

    While a key has spooled events, new events for that key queue behind them (per-key order).
    As with Kafka itself, a caller must publish same-key events one after another."""

    def __init__(
        self,
        broker: Broker,
        spool: FileSpool,
        breaker: Breaker,
        drain_per_second: float,
        clock: Callable[[], float] = time.monotonic,
    ):
        self.broker, self.spool, self.breaker, self.clock = broker, spool, breaker, clock
        self.drain_per_second, self.tokens, self.last_refill = drain_per_second, 0.0, clock()
        self.spooled_per_key: dict[str, int] = {}
        self._lock = threading.Lock()
        for e in spool.pending():  # after a restart
            self.spooled_per_key[e.key] = self.spooled_per_key.get(e.key, 0) + 1

    def publish(self, e: Event) -> str:
        with self._lock:
            if e.key in self.spooled_per_key or not self.breaker.allow():
                return self._spool(e)
        try:
            self.broker(e)
        except Exception:
            self.breaker.failure()
            with self._lock:
                return self._spool(e)
        self.breaker.success()
        return "SENT"

    def _spool(self, e: Event) -> str:
        self.spool.append(e)
        self.spooled_per_key[e.key] = self.spooled_per_key.get(e.key, 0) + 1
        return "SPOOLED"

    def drain_once(self) -> int:
        """Run on a schedule (e.g. every 100 ms) from one background thread."""
        with self._lock:
            now = self.clock()
            self.tokens = min(
                self.drain_per_second,
                self.tokens + (now - self.last_refill) * self.drain_per_second,
            )
            self.last_refill = now
            sent = 0
            while self.tokens >= 1 and self.spool.size() > 0 and self.breaker.allow():
                e = self.spool.peek()
                try:
                    self.broker(e)
                except Exception:
                    self.breaker.failure()
                    break
                self.breaker.success()
                self.spool.remove_head()
                n = self.spooled_per_key[e.key] - 1
                if n:
                    self.spooled_per_key[e.key] = n
                else:
                    del self.spooled_per_key[e.key]
                self.tokens -= 1
                sent += 1
            return sent

    def backlog(self) -> int:
        with self._lock:
            return self.spool.size()

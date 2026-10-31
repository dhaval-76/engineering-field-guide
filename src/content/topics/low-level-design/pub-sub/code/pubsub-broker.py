import threading
from dataclasses import dataclass, field

@dataclass(frozen=True)
class Message:
    offset: int
    key: str
    payload: str

@dataclass
class _Topic:
    log: list[Message] = field(default_factory=list)
    base_offset: int = 0                                  # offset of log[0] after retention trims
    committed: dict[str, int] = field(default_factory=dict)
    lock: threading.Lock = field(default_factory=threading.Lock)

class Broker:
    """In-memory log-based pub/sub: each topic is an append-only log; each consumer group has its own offset."""

    def __init__(self):
        self._topics: dict[str, _Topic] = {}
        self._lock = threading.Lock()

    def _topic(self, name: str) -> _Topic:
        with self._lock:
            return self._topics.setdefault(name, _Topic())

    def publish(self, topic: str, key: str, payload: str) -> int:
        t = self._topic(topic)
        with t.lock:
            offset = t.base_offset + len(t.log)
            t.log.append(Message(offset, key, payload))
            return offset

    def subscribe(self, topic: str, group: str, from_beginning: bool = False) -> None:
        t = self._topic(topic)
        with t.lock:
            t.committed.setdefault(group, t.base_offset if from_beginning else t.base_offset + len(t.log))

    def poll(self, topic: str, group: str, max_messages: int) -> list[Message]:
        t = self._topic(topic)
        with t.lock:
            if group not in t.committed:
                raise KeyError(f"not subscribed: {group}")
            start = t.committed[group] - t.base_offset
            return t.log[start:start + max_messages]

    def commit(self, topic: str, group: str, next_offset: int) -> None:
        t = self._topic(topic)
        with t.lock:
            t.committed[group] = max(t.committed.get(group, 0), next_offset)

    def trim(self, topic: str) -> int:
        t = self._topic(topic)
        with t.lock:
            end = t.base_offset + len(t.log)
            min_committed = min(t.committed.values(), default=end)
            drop = min_committed - t.base_offset
            del t.log[:drop]
            t.base_offset = min_committed
            return drop

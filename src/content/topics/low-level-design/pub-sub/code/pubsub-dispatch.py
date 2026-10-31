# uses Broker from above
import threading
import time

class Dispatcher:
    """Push delivery for one group: poll, hand to the handler, retry with backoff, dead-letter, then commit."""

    def __init__(self, broker, topic: str, group: str, handler, max_attempts: int = 5):
        self.broker, self.topic, self.group = broker, topic, group
        self.handler, self.max_attempts = handler, max_attempts
        self._stop = threading.Event()
        broker.subscribe(topic, group, from_beginning=False)

    def run(self) -> None:
        while not self._stop.is_set():
            batch = self.broker.poll(self.topic, self.group, 100)
            if not batch:
                self._stop.wait(0.05)
                continue
            for m in batch:
                self._deliver(m)
                self.broker.commit(self.topic, self.group, m.offset + 1)   # commit after handling: at-least-once

    def _deliver(self, m) -> None:
        for attempt in range(1, self.max_attempts + 1):
            try:
                self.handler(m)
                return
            except Exception:
                if attempt == self.max_attempts:
                    self.broker.publish(f"{self.topic}.dlq", m.key, m.payload)   # poison message: move on
                    return
                time.sleep(0.1 * 2 ** attempt)                                # exponential backoff

    def stop(self) -> None:
        self._stop.set()

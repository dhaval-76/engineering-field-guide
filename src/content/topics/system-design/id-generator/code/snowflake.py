import threading, time

EPOCH_MS = 1_704_067_200_000          # 2024-01-01T00:00:00Z
MAX_SEQUENCE = (1 << 12) - 1

class Snowflake:
    def __init__(self, worker: int):
        if not 0 <= worker <= 1023:
            raise ValueError("worker 0–1023")
        self.worker = worker
        self.last_ms = -1
        self.sequence = 0
        self.lock = threading.Lock()

    def next_id(self) -> int:
        with self.lock:
            now = time.time_ns() // 1_000_000
            if now < self.last_ms:
                if self.last_ms - now > 10:
                    raise RuntimeError(f"clock moved back {self.last_ms - now} ms")
                now = self._wait_after(self.last_ms - 1)
            if now == self.last_ms:
                self.sequence = (self.sequence + 1) & MAX_SEQUENCE
                if self.sequence == 0:
                    now = self._wait_after(self.last_ms)
            else:
                self.sequence = 0
            self.last_ms = now
            return ((now - EPOCH_MS) << 22) | (self.worker << 12) | self.sequence

    @staticmethod
    def _wait_after(ms: int) -> int:
        while (now := time.time_ns() // 1_000_000) <= ms:
            pass
        return now

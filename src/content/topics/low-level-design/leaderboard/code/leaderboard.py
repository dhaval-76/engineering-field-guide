import bisect
import threading

class Leaderboard:
    """Higher score first; ties go to whoever reached the score first.
    Sorted list of keys (-score, reached_at, player): bisect finds positions in O(log n);
    insertion shifts elements (O(n)). Use sortedcontainers.SortedList or Redis for large boards."""

    def __init__(self):
        self._key: dict[str, tuple[int, int, str]] = {}
        self._ranking: list[tuple[int, int, str]] = []
        self._clock = 0
        self._lock = threading.Lock()

    def add_score(self, player: str, delta: int) -> int:
        with self._lock:
            old = self._key.get(player)
            score = delta
            if old is not None:
                self._ranking.pop(bisect.bisect_left(self._ranking, old))
                score += -old[0]
            self._clock += 1
            key = (-score, self._clock, player)
            self._key[player] = key
            bisect.insort(self._ranking, key)
            return score

    def top(self, k: int) -> list[tuple[str, int]]:
        return [(player, -neg) for neg, _, player in self._ranking[:k]]

    def rank(self, player: str) -> int:
        key = self._key.get(player)
        return -1 if key is None else bisect.bisect_left(self._ranking, key) + 1

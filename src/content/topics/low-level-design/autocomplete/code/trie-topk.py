import threading

K = 5

class _Node:
    __slots__ = ("next", "top")

    def __init__(self):
        self.next: dict[str, "_Node"] = {}
        self.top: list[tuple[int, str]] = []        # (-score, word): sorts best first

class RankedAutocomplete:
    """Every node caches its top K completions, so a query is O(prefix length)."""

    def __init__(self):
        self._root = _Node()
        self._scores: dict[str, int] = {}
        self._lock = threading.Lock()

    def record(self, phrase: str, delta: int) -> None:
        """Add `delta` to a phrase's score (e.g. +1 per search) and refresh the caches on its path."""
        p = phrase.strip().lower()
        with self._lock:
            score = self._scores[p] = self._scores.get(p, 0) + delta
            n = self._root
            self._update(n, p, score)
            for c in p:
                n = n.next.setdefault(c, _Node())
                self._update(n, p, score)

    @staticmethod
    def _update(n: _Node, word: str, score: int) -> None:
        n.top = sorted([e for e in n.top if e[1] != word] + [(-score, word)])[:K]

    def suggest(self, prefix: str) -> list[str]:
        with self._lock:
            n = self._root
            for c in prefix.lower():
                n = n.next.get(c)
                if n is None:
                    return []
            return [w for _, w in n.top]

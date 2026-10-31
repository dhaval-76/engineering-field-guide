import itertools
import random
import threading

class RoundRobin:
    def __init__(self, servers: list[str]):
        self._cycle = itertools.cycle(list(servers))
        self._lock = threading.Lock()

    def pick(self) -> str:
        with self._lock:
            return next(self._cycle)

    def done(self, server: str) -> None:
        pass

class SmoothWeighted:
    """nginx's smooth weighted round robin: weights 5,1,1 → a a b a c a a."""

    def __init__(self, weights: dict[str, int]):
        self.weights = dict(weights)
        self.current = {s: 0 for s in weights}
        self._lock = threading.Lock()

    def pick(self) -> str:
        with self._lock:
            for s, w in self.weights.items():
                self.current[s] += w
            # first max wins ties
            best = max(self.weights, key=lambda s: self.current[s])
            self.current[best] -= sum(self.weights.values())
            return best

    def done(self, server: str) -> None:
        pass

class LeastConnections:
    def __init__(self, servers: list[str]):
        self.active = {s: 0 for s in servers}
        self._lock = threading.Lock()

    def pick(self) -> str:
        with self._lock:
            s = min(self.active, key=self.active.get)
            self.active[s] += 1
            return s

    def done(self, server: str) -> None:
        with self._lock:
            self.active[server] -= 1

class PowerOfTwo:
    """Sample two servers at random, send to the less loaded one."""

    def __init__(self, servers: list[str], rnd: random.Random | None = None):
        self.servers = list(servers)
        self.active = {s: 0 for s in servers}
        self.rnd = rnd or random.Random()
        self._lock = threading.Lock()

    def pick(self) -> str:
        with self._lock:
            a, b = self.rnd.choice(self.servers), self.rnd.choice(self.servers)
            s = a if self.active[a] <= self.active[b] else b
            self.active[s] += 1
            return s

    def done(self, server: str) -> None:
        with self._lock:
            self.active[server] -= 1

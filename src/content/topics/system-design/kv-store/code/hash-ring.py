import bisect, hashlib

class HashRing:
    def __init__(self, vnodes: int = 128):
        self.vnodes = vnodes
        self.tokens: list[int] = []          # sorted ring positions
        self.owner: dict[int, str] = {}      # token → physical node

    @staticmethod
    def _hash(s: str) -> int:
        return int.from_bytes(hashlib.md5(s.encode()).digest()[:8], "big")

    def add_node(self, node: str) -> None:
        for i in range(self.vnodes):
            t = self._hash(f"{node}#{i}")
            bisect.insort(self.tokens, t)
            self.owner[t] = node

    def remove_node(self, node: str) -> None:
        self.tokens = [t for t in self.tokens if self.owner[t] != node]
        self.owner = {t: n for t, n in self.owner.items() if n != node}

    def replicas_for(self, key: str, n: int) -> list[str]:
        start = bisect.bisect_left(self.tokens, self._hash(key))
        out: list[str] = []
        for i in range(len(self.tokens)):
            node = self.owner[self.tokens[(start + i) % len(self.tokens)]]
            if node not in out:
                out.append(node)
                if len(out) == n:
                    break
        return out

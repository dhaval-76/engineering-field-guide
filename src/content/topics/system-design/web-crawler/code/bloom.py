import hashlib
import math

class BloomFilter:
    """'Have we seen this URL?' in ~10 bits per URL. No false negatives; a tunable rate of false positives."""

    def __init__(self, expected: int, fp_rate: float):
        self.m = math.ceil(-expected * math.log(fp_rate) / math.log(2) ** 2)   # bits
        self.k = max(1, round(self.m / expected * math.log(2)))                 # hash functions
        self.bits = bytearray((self.m + 7) // 8)

    def _positions(self, item: str):
        d = hashlib.sha256(item.encode()).digest()
        h1, h2 = int.from_bytes(d[:8], "big"), int.from_bytes(d[8:16], "big") | 1
        return ((h1 + i * h2) % self.m for i in range(self.k))                   # double hashing

    def add(self, item: str) -> None:
        for p in self._positions(item):
            self.bits[p >> 3] |= 1 << (p & 7)

    def might_contain(self, item: str) -> bool:
        return all(self.bits[p >> 3] & (1 << (p & 7)) for p in self._positions(item))

import hashlib
from datetime import datetime, timezone

# Unique visitors per hour with Redis HyperLogLog (redis-py): ~12 KB per key whatever the
# cardinality.
def track(r, user_id: str, at: datetime | None = None) -> None:
    key = f"uv:{(at or datetime.now(timezone.utc)):%Y-%m-%dT%H}"
    r.pfadd(key, user_id)
    r.expire(key, 2 * 24 * 3600)

def unique_across(r, hour_keys: list[str]) -> int:
    return r.pfcount(*hour_keys)                        # merges the hours

class TopK:
    """Approximate top-K: a Count-Min Sketch estimates counts; a small dict keeps the current leaders."""

    def __init__(self, k: int, depth: int = 4, width: int = 1 << 16):
        self.k, self.depth, self.width = k, depth, width
        self.table = [[0] * width for _ in range(depth)]
        self.leaders: dict[str, int] = {}

    def _col(self, row: int, item: str) -> int:
        h = hashlib.blake2b(item.encode(), digest_size=8, salt=row.to_bytes(8, "big")).digest()
        return int.from_bytes(h, "big") % self.width

    def add(self, item: str) -> None:
        estimate = None
        for row in range(self.depth):
            col = self._col(row, item)
            self.table[row][col] += 1
            c = self.table[row][col]
            # min over rows limits over-counting
            estimate = c if estimate is None else min(estimate, c)
        self.leaders[item] = estimate
        # drop the smallest leader
        if len(self.leaders) > self.k:
            del self.leaders[min(self.leaders, key=self.leaders.get)]

    def top(self) -> list[tuple[str, int]]:
        return sorted(self.leaders.items(), key=lambda kv: kv[1], reverse=True)
# Per partition: keep a TopK each; every few seconds merge the partitions' candidates for
# the global top-K.

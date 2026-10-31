import threading

class OffsetStore:
    """Committed offsets per (group, topic, partition). Real systems store these in a compacted log."""

    def __init__(self):
        self._committed: dict[tuple[str, str, int], int] = {}
        self._lock = threading.Lock()

    def commit(self, group: str, topic: str, partition: int, last_processed: int) -> None:
        """Commit the NEXT offset to read — last processed + 1; never move backwards by accident."""
        k = (group, topic, partition)
        with self._lock:
            self._committed[k] = max(self._committed.get(k, 0), last_processed + 1)

    def start_offset(self, group, topic, partition, log_start: int, log_end: int, earliest: bool) -> int:
        c = self._committed.get((group, topic, partition))
        if c is None or c < log_start:              # new group, or the data expired
            return log_start if earliest else log_end
        return c

def assign(members: list[str], partitions: int) -> dict[str, list[int]]:
    """Range assignment: partitions split as evenly as possible across sorted members."""
    ordered = sorted(members)
    out = {m: [] for m in ordered}
    for p in range(partitions):
        out[ordered[p % len(ordered)]].append(p)
    # members beyond the partition count get nothing
    return out

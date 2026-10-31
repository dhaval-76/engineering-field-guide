import bisect
import time
from dataclasses import dataclass, field

SEGMENT_BYTES = 1 << 20     # roll at 1 MB here (1 GB in Kafka)
INDEX_EVERY_BYTES = 4096    # one index entry per ~4 KB

@dataclass
class Segment:
    base_offset: int
    records: list = field(default_factory=list)       # (offset, key, value)
    index_offsets: list = field(default_factory=list)  # sparse index: offsets…
    index_slots: list = field(default_factory=list)    # …and where they sit
    bytes: int = 0
    last_indexed: int = -INDEX_EVERY_BYTES
    modified: float = 0.0

class PartitionLog:
    """One partition, in memory for clarity: segments named by base offset, each with a sparse index.
    A real broker keeps each segment as a file plus an index file; the lookups are the same."""

    def __init__(self):
        self.segments: list[Segment] = [Segment(0)]
        self.next_offset = 0

    def append(self, key: bytes | None, value: bytes, now: float | None = None) -> int:
        seg = self.segments[-1]
        if seg.bytes >= SEGMENT_BYTES:
            seg = Segment(self.next_offset)
            self.segments.append(seg)
        position = seg.bytes
        seg.records.append((self.next_offset, key, value))
        if position - seg.last_indexed >= INDEX_EVERY_BYTES:
            seg.index_offsets.append(self.next_offset)
            seg.index_slots.append(len(seg.records) - 1)
            seg.last_indexed = position
        seg.bytes += len(key or b"") + len(value) + 16      # record header + payload
        seg.modified = now if now is not None else time.time()
        self.next_offset += 1
        return self.next_offset - 1

    def read(self, from_offset: int, max_records: int = 100) -> list:
        # which segment
        i = bisect.bisect_right([s.base_offset for s in self.segments], from_offset) - 1
        if i < 0:
            return []
        seg = self.segments[i]
        # sparse index lookup
        j = bisect.bisect_right(seg.index_offsets, from_offset) - 1
        start = seg.index_slots[j] if j >= 0 else 0
        # short scan
        return [r for r in seg.records[start:] if r[0] >= from_offset][:max_records]

    def enforce_retention(self, cutoff: float) -> None:
        """Delete whole old segments — never individual messages."""
        active = self.segments[-1]
        self.segments = [s for s in self.segments if s is active or s.modified >= cutoff]

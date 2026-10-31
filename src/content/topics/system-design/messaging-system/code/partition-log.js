// One partition, in memory for clarity: segments named by base offset, each with a sparse
// index. A real broker keeps each segment as a file plus an index file; the lookups are
// the same.
const SEGMENT_BYTES = 1 << 20; // roll at 1 MB here (1 GB in Kafka)
const INDEX_EVERY_BYTES = 4096; // one index entry per ~4 KB

export class PartitionLog {
  segments = []; // sorted by baseOffset
  nextOffset = 0;

  constructor() {
    this.#roll();
  }

  append(key, value, now = Date.now()) {
    let seg = this.segments.at(-1);
    if (seg.bytes >= SEGMENT_BYTES) seg = this.#roll();
    // record header + payload
    const size = (key?.length ?? 0) + value.length + 16;
    const position = seg.bytes;
    seg.records.push({ offset: this.nextOffset, key, value, position });
    if (position - seg.lastIndexed >= INDEX_EVERY_BYTES || seg.index.length === 0) {
      // sparse: offset → slot
      seg.index.push([this.nextOffset, seg.records.length - 1]);
      seg.lastIndexed = position;
    }
    seg.bytes += size;
    seg.modified = now;
    return this.nextOffset++;
  }

  read(fromOffset, maxRecords = 100) {
    // which segment holds it
    const seg = floor(this.segments, fromOffset, (s) => s.baseOffset);
    if (!seg) return [];
    // binary search the sparse index
    const near = floor(seg.index, fromOffset, (e) => e[0]);
    const out = [];
    for (let i = near ? near[1] : 0; i < seg.records.length && out.length < maxRecords; i++)
      // short sequential scan
      if (seg.records[i].offset >= fromOffset) out.push(seg.records[i]);
    return out;
  }

  enforceRetention(cutoff) {
    // delete whole old segments — never individual messages
    const active = this.segments.at(-1);
    this.segments = this.segments.filter((s) => s === active || s.modified >= cutoff);
  }

  #roll() {
    const seg = {
      baseOffset: this.nextOffset,
      records: [],
      index: [],
      bytes: 0,
      lastIndexed: 0,
      modified: 0,
    };
    this.segments.push(seg);
    return seg;
  }
}

function floor(sorted, target, keyOf) {
  // last element with key ≤ target
  let lo = 0,
    hi = sorted.length - 1,
    best = null;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    if (keyOf(sorted[mid]) <= target) {
      best = sorted[mid];
      lo = mid + 1;
    } else hi = mid - 1;
  }
  return best;
}

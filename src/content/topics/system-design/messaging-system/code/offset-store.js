// Committed offsets per (group, topic, partition). Real systems store these as records in
// a compacted log.
export class OffsetStore {
  #committed = new Map();

  // Commit the NEXT offset to read — last processed + 1; never move backwards by
  // accident.
  commit(group, topic, partition, lastProcessed) {
    const k = `${group}|${topic}|${partition}`;
    this.#committed.set(k, Math.max(this.#committed.get(k) ?? 0, lastProcessed + 1));
  }

  // Where a consumer starts: committed position, or the reset policy for a new group.
  startOffset(group, topic, partition, { logStart, logEnd, earliest }) {
    const c = this.#committed.get(`${group}|${topic}|${partition}`);
    // new group, or data expired
    if (c === undefined || c < logStart) return earliest ? logStart : logEnd;
    return c;
  }
}

// Range assignment: partitions split as evenly as possible across sorted members.
export function assign(members, partitions) {
  const sorted = [...members].sort();
  const out = new Map(sorted.map((m) => [m, []]));
  for (let p = 0; p < partitions; p++) out.get(sorted[p % sorted.length]).push(p);
  return out; // members beyond the partition count get nothing
}

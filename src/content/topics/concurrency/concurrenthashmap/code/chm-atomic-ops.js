// Node: a plain Map is fine within one process (single-threaded event loop) as long as
// read-modify-write doesn't span an await:
counts.set(word, (counts.get(word) ?? 0) + 1);      // atomic — no await in between

// "Load once per key" across concurrent async calls: cache the PROMISE, not the value.
const inflight = new Map();
export function getProfile(id) {
  if (!inflight.has(id)) inflight.set(id, loadProfile(id).finally(() => setTimeout(() => inflight.delete(id), 60_000)));
  return inflight.get(id);                           // concurrent callers share one load
}

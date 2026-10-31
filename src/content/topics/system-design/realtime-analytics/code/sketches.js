// Unique visitors per hour with Redis HyperLogLog (ioredis): ~12 KB per key whatever the
// cardinality.
export async function track(redis, userId, at = new Date()) {
  const key = `uv:${at.toISOString().slice(0, 13)}`; // e.g. uv:2026-10-31T09
  await redis.pfadd(key, userId);
  await redis.expire(key, 2 * 24 * 3600);
}
// merges the hours
export const uniqueAcross = (redis, hourKeys) => redis.pfcount(...hourKeys);

// Approximate top-K: a Count-Min Sketch estimates counts; a small map keeps the current
// leaders.
const fnv = (s, seed) => {
  let h = 2166136261 ^ seed;
  for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619);
  return h >>> 0;
};

export class TopK {
  #depth = 4;
  #width = 1 << 16;
  #table = Array.from({ length: 4 }, () => new Uint32Array(1 << 16));
  #leaders = new Map();
  constructor(k) {
    this.k = k;
  }
  add(item) {
    let estimate = Infinity;
    for (let row = 0; row < this.#depth; row++) {
      const col = fnv(item, row * 0x9e3779b9) % this.#width;
      // min over rows limits over-counting
      estimate = Math.min(estimate, ++this.#table[row][col]);
    }
    this.#leaders.set(item, estimate);
    if (this.#leaders.size > this.k) {                        // drop the smallest leader
      let min = null;
      for (const [i, c] of this.#leaders) if (min === null || c < this.#leaders.get(min)) min = i;
      this.#leaders.delete(min);
    }
  }
  top() {
    return [...this.#leaders].sort((a, b) => b[1] - a[1]);
  }
}
// Per partition: keep a TopK each; every few seconds merge the partitions' candidates for
// the global top-K.

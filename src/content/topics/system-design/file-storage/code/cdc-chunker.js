import { createHash } from 'node:crypto';

const M64 = (1n << 64n) - 1n;
const GEAR = (() => {
  // xorshift64*: the same table on every client so everyone cuts at the same places
  let x = 0x9e3779b97f4a7c15n;
  const t = [];
  for (let i = 0; i < 256; i++) {
    x ^= x >> 12n;
    x = (x ^ (x << 25n)) & M64;
    x ^= x >> 27n;
    t.push((x * 0x2545f4914f6cdd1dn) & M64);
  }
  return t;
})();

// Content-defined chunking with a gear rolling hash (BigInt keeps 64-bit arithmetic
// exact).
export function chunk(data, minSize = 2048, avgBits = 13, maxSize = 65536) {
  const mask = ((1n << BigInt(avgBits)) - 1n) << BigInt(64 - avgBits);
  const chunks = [];
  let start = 0;
  let h = 0n;
  for (let i = 0; i < data.length; i++) {
    h = ((h << 1n) + GEAR[data[i]]) & M64;
    const size = i - start + 1;
    if ((size >= minSize && (h & mask) === 0n) || size >= maxSize) {
      chunks.push(data.subarray(start, i + 1));
      start = i + 1;
      h = 0n;
    }
  }
  if (start < data.length) chunks.push(data.subarray(start));
  return chunks;
}

export const chunkIds = (data) => chunk(data).map((c) => createHash('sha256').update(c).digest('hex'));

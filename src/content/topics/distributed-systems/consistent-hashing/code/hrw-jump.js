import { createHash } from 'node:crypto';

const MASK = (1n << 64n) - 1n;
export const hash64 = (s) => createHash('md5').update(s).digest().readBigUInt64BE(0); // unsigned BigInt

// Rendezvous (highest-random-weight) hashing: the node with the highest score for this key wins.
export function rendezvous(key, nodes) {
  let best = null;
  let bestScore = -1n;
  for (const node of nodes) {
    const score = hash64(`${node}|${key}`);
    if (score > bestScore) [best, bestScore] = [node, score];
  }
  return best;
}

// Jump consistent hash: 64-bit arithmetic needs BigInt in JavaScript.
export function jumpHash(key, buckets) {
  let b = -1n;
  let j = 0n;
  let k = BigInt.asUintN(64, key);
  while (j < BigInt(buckets)) {
    b = j;
    k = (k * 2862933555777941757n + 1n) & MASK;
    j = BigInt(Math.floor(Number(b + 1n) * (2 ** 31 / Number((k >> 33n) + 1n))));
  }
  return Number(b);
}

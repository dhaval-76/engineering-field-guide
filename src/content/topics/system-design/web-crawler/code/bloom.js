import { createHash } from 'node:crypto';

// "Have we seen this URL?" in ~10 bits per URL. No false negatives; a tunable rate of false positives.
export class BloomFilter {
  constructor(expected, fpRate) {
    this.m = Math.ceil((-expected * Math.log(fpRate)) / Math.log(2) ** 2);
    this.k = Math.max(1, Math.round((this.m / expected) * Math.log(2)));
    this.bits = new Uint8Array(Math.ceil(this.m / 8));
  }

  *#positions(item) {
    const d = createHash('sha256').update(item).digest();
    const h1 = d.readBigUInt64BE(0);
    const h2 = d.readBigUInt64BE(8) | 1n;
    const m = BigInt(this.m);
    for (let i = 0n; i < BigInt(this.k); i++) yield Number((h1 + i * h2) % m); // double hashing
  }

  add(item) {
    for (const p of this.#positions(item)) this.bits[p >> 3] |= 1 << (p & 7);
  }

  mightContain(item) {
    for (const p of this.#positions(item)) if (!(this.bits[p >> 3] & (1 << (p & 7)))) return false;
    return true;
  }
}

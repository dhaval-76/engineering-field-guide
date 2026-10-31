import { createHash } from 'node:crypto';

const hash = (s) => createHash('md5').update(s).digest().readBigUInt64BE(0); // stable 64-bit position

export class HashRing {
  #tokens = []; // sorted BigInt positions
  #owner = new Map(); // token -> physical node

  constructor(virtualNodesPerNode = 128) {
    this.vnodes = virtualNodesPerNode;
  }

  addNode(node) {
    for (let i = 0; i < this.vnodes; i++) {
      const t = hash(`${node}#${i}`);
      this.#owner.set(t, node);
      this.#tokens.splice(this.#indexAtOrAfter(t), 0, t);
    }
  }

  removeNode(node) {
    this.#tokens = this.#tokens.filter((t) => this.#owner.get(t) !== node); // only its arcs change owner
    for (const [t, n] of this.#owner) if (n === node) this.#owner.delete(t);
  }

  // The first n DISTINCT physical nodes clockwise from the key's position.
  replicasFor(key, n) {
    if (this.#tokens.length === 0) throw new Error('empty ring');
    const start = this.#indexAtOrAfter(hash(key));
    const out = [];
    for (let i = 0; i < this.#tokens.length && out.length < n; i++) {
      const node = this.#owner.get(this.#tokens[(start + i) % this.#tokens.length]);
      if (!out.includes(node)) out.push(node);
    }
    return out;
  }

  #indexAtOrAfter(t) {
    let lo = 0;
    let hi = this.#tokens.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (this.#tokens[mid] < t) lo = mid + 1;
      else hi = mid;
    }
    return lo;
  }
}

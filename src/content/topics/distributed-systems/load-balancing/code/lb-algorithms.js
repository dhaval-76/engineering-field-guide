export class RoundRobin {
  #i = 0;
  constructor(servers) {
    this.servers = [...servers];
  }
  pick() {
    return this.servers[this.#i++ % this.servers.length];
  }
  done() {}
}

// Smooth weighted round robin (nginx): weights 5,1,1 → a a b a c a a.
export class SmoothWeighted {
  constructor(weights) {
    this.weights = new Map(Object.entries(weights));
    this.current = new Map([...this.weights.keys()].map((s) => [s, 0]));
  }
  pick() {
    let total = 0;
    let best = null;
    for (const [s, w] of this.weights) {
      this.current.set(s, this.current.get(s) + w);
      total += w;
      if (best === null || this.current.get(s) > this.current.get(best)) best = s;
    }
    this.current.set(best, this.current.get(best) - total);
    return best;
  }
  done() {}
}

export class LeastConnections {
  constructor(servers) {
    this.active = new Map(servers.map((s) => [s, 0]));
  }
  pick() {
    let best = null;
    for (const [s, n] of this.active) if (best === null || n < this.active.get(best)) best = s;
    this.active.set(best, this.active.get(best) + 1);
    return best;
  }
  done(s) {
    this.active.set(s, this.active.get(s) - 1);
  }
}

// Power of two choices: two random candidates, take the less loaded.
export class PowerOfTwo {
  constructor(servers, random = Math.random) {
    this.servers = [...servers];
    this.random = random;
    this.active = new Map(servers.map((s) => [s, 0]));
  }
  pick() {
    const r = () => this.servers[Math.floor(this.random() * this.servers.length)];
    const a = r();
    const b = r();
    const s = this.active.get(a) <= this.active.get(b) ? a : b;
    this.active.set(s, this.active.get(s) + 1);
    return s;
  }
  done(s) {
    this.active.set(s, this.active.get(s) - 1);
  }
}

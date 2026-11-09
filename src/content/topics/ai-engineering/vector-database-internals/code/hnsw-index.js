/** A small binary heap ordered by `dist`; `sign` of -1 makes it a max-heap. */
class Heap {
  constructor(sign) {
    this.sign = sign;
    this.items = [];
  }
  get size() {
    return this.items.length;
  }
  peek() {
    return this.items[0];
  }
  less(a, b) {
    return this.sign * this.items[a].dist < this.sign * this.items[b].dist;
  }
  swap(a, b) {
    [this.items[a], this.items[b]] = [this.items[b], this.items[a]];
  }
  push(item) {
    this.items.push(item);
    let i = this.size - 1;
    while (i > 0 && this.less(i, (i - 1) >> 1)) {
      this.swap(i, (i - 1) >> 1);
      i = (i - 1) >> 1;
    }
  }
  pop() {
    const top = this.items[0];
    const last = this.items.pop();
    if (this.size) {
      this.items[0] = last;
      for (let i = 0; ; ) {
        let s = i;
        for (const c of [2 * i + 1, 2 * i + 2]) if (c < this.size && this.less(c, s)) s = c;
        if (s === i) break;
        this.swap(i, s);
        i = s;
      }
    }
    return top;
  }
}

/** A small HNSW index over vectors, using squared Euclidean distance. */
class Hnsw {
  constructor(m = 8, efConstruction = 64, random = Math.random) {
    this.m = m; // links per node on upper layers; layer 0 allows 2 * m
    this.efConstruction = efConstruction;
    this.levelFactor = 1 / Math.log(m);
    this.random = random;
    this.vectors = [];
    this.links = []; // node -> layer -> neighbours
    this.entry = -1;
  }

  distance(a, b) {
    let sum = 0;
    for (let i = 0; i < a.length; i++) sum += (a[i] - b[i]) ** 2;
    return sum;
  }

  topLevel(node) {
    return this.links[node].length - 1;
  }

  /** Best-first search of one layer, returning up to ef hits, nearest first. */
  searchLayer(query, start, ef, layer) {
    const visited = new Set([start]);
    const first = { id: start, dist: this.distance(query, this.vectors[start]) };
    const frontier = new Heap(1);
    const best = new Heap(-1);
    frontier.push(first);
    best.push(first);
    while (frontier.size) {
      const current = frontier.pop();
      // Everything left to explore is further than the worst result we are keeping.
      if (best.size >= ef && current.dist > best.peek().dist) break;
      for (const next of this.links[current.id][layer]) {
        if (visited.has(next)) continue;
        visited.add(next);
        const dist = this.distance(query, this.vectors[next]);
        if (best.size < ef || dist < best.peek().dist) {
          const hit = { id: next, dist };
          frontier.push(hit);
          best.push(hit);
          if (best.size > ef) best.pop();
        }
      }
    }
    return best.items.sort((a, b) => a.dist - b.dist);
  }

  /** Walks down from the top layer to just above `target`, keeping only the closest node. */
  descend(query, target) {
    let node = this.entry;
    for (let layer = this.topLevel(this.entry); layer > target; layer--) {
      node = this.searchLayer(query, node, 1, layer)[0].id;
    }
    return node;
  }

  add(vector) {
    const id = this.vectors.length;
    // Most nodes stay on layer 0; each higher layer holds about 1/m of the one below.
    const level = Math.floor(-Math.log(1 - this.random()) * this.levelFactor);
    this.vectors.push(vector);
    const own = Array.from({ length: level + 1 }, () => []);
    this.links.push(own);
    if (this.entry === -1) {
      this.entry = id;
      return id;
    }
    const oldTop = this.topLevel(this.entry);
    let node = this.descend(vector, level);
    for (let layer = Math.min(level, oldTop); layer >= 0; layer--) {
      const near = this.searchLayer(vector, node, this.efConstruction, layer);
      const limit = layer === 0 ? 2 * this.m : this.m;
      for (const hit of near.slice(0, this.m)) {
        own[layer].push(hit.id);
        const theirs = this.links[hit.id][layer];
        theirs.push(id);
        if (theirs.length > limit) this.prune(hit.id, layer, limit);
      }
      node = near[0].id;
    }
    if (level > oldTop) this.entry = id;
    return id;
  }

  /** Keeps a node's closest neighbours when it has gained too many links. */
  prune(node, layer, limit) {
    const from = this.vectors[node];
    this.links[node][layer] = this.links[node][layer]
      .map((n) => ({ n, d: this.distance(from, this.vectors[n]) }))
      .sort((a, b) => a.d - b.d)
      .slice(0, limit)
      .map((x) => x.n);
  }

  search(query, k, efSearch) {
    if (this.entry === -1) return [];
    return this.searchLayer(query, this.descend(query, 0), Math.max(efSearch, k), 0).slice(0, k);
  }
}

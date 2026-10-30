class TimeMap {
  #history = new Map();
  set(key, value, timestamp) {
    if (!this.#history.has(key)) this.#history.set(key, []);
    this.#history.get(key).push([timestamp, value]);
  }
  get(key, t) {
    const v = this.#history.get(key) ?? [];
    let lo = 0, hi = v.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (v[mid][0] > t) hi = mid; else lo = mid + 1;
    }
    return lo === 0 ? '' : v[lo - 1][1];
  }
}

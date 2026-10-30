class RandomizedSet {
  #indexOf = new Map();
  #values = [];
  insert(val) {
    if (this.#indexOf.has(val)) return false;
    this.#indexOf.set(val, this.#values.length);
    this.#values.push(val);
    return true;
  }
  remove(val) {
    if (!this.#indexOf.has(val)) return false;
    const i = this.#indexOf.get(val), last = this.#values.pop();
    this.#indexOf.delete(val);
    if (i < this.#values.length) {
      this.#values[i] = last;
      this.#indexOf.set(last, i);
    }
    return true;
  }
  getRandom() { return this.#values[Math.floor(Math.random() * this.#values.length)]; }
}

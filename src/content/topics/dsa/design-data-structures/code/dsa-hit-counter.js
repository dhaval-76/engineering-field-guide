// Queue of [second, count]: O(1) amortised, memory proportional to distinct seconds in
// the window.
class HitCounter {
  static WINDOW = 300;
  #buckets = [];
  #head = 0;          // index of the oldest bucket (avoids O(n) shift)
  #total = 0;

  #evict(now) {
    const expired = (i) => now - this.#buckets[i][0] >= HitCounter.WINDOW;
    while (this.#head < this.#buckets.length && expired(this.#head)) {
      this.#total -= this.#buckets[this.#head++][1];
    }
    if (this.#head > 1024 && this.#head * 2 > this.#buckets.length) {
      // compact so memory tracks the window
      this.#buckets = this.#buckets.slice(this.#head);
      this.#head = 0;
    }
  }

  hit(timestamp) {
    this.#evict(timestamp);
    const last = this.#buckets[this.#buckets.length - 1];
    if (this.#head < this.#buckets.length && last[0] === timestamp) last[1]++;
    else this.#buckets.push([timestamp, 1]);
    this.#total++;
  }

  getHits(timestamp) {
    this.#evict(timestamp);
    return this.#total;
  }
}

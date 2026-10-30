// JavaScript has no built-in priority queue; this one takes a comparator.
class Heap {
  #a = [];
  constructor(less = (x, y) => x < y) { this.less = less; }
  get size() { return this.#a.length; }
  peek() { return this.#a[0]; }
  push(x) {
    const a = this.#a;
    a.push(x);
    let i = a.length - 1;
    while (i > 0 && this.less(a[i], a[(i - 1) >> 1])) {
      [a[i], a[(i - 1) >> 1]] = [a[(i - 1) >> 1], a[i]];
      i = (i - 1) >> 1;
    }
  }
  pop() {
    const a = this.#a, top = a[0], last = a.pop();
    if (a.length) {
      a[0] = last;
      let i = 0;
      for (;;) {
        const l = 2 * i + 1, r = l + 1;
        let s = i;
        if (l < a.length && this.less(a[l], a[s])) s = l;
        if (r < a.length && this.less(a[r], a[s])) s = r;
        if (s === i) break;
        [a[i], a[s]] = [a[s], a[i]];
        i = s;
      }
    }
    return top;
  }
}

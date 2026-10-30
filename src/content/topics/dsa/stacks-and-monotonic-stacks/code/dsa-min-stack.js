class MinStack {
  #stack = [];                                   // [value, minSoFar]
  push(x) {
    const min = this.#stack.length ? Math.min(x, this.#stack.at(-1)[1]) : x;
    this.#stack.push([x, min]);
  }
  pop() { this.#stack.pop(); }
  top() { return this.#stack.at(-1)[0]; }
  getMin() { return this.#stack.at(-1)[1]; }
}

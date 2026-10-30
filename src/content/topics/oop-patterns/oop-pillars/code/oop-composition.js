export class Stack {
  #items = []; // has-a array; callers cannot splice into the middle
  push(item) { this.#items.push(item); }
  pop() { return this.#items.pop(); }
  peek() { return this.#items.at(-1); }
  get isEmpty() { return this.#items.length === 0; }
}

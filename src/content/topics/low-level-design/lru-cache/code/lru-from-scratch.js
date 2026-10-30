class Node {
  constructor(key, value) {
    this.key = key; // kept so eviction can also delete the map entry
    this.value = value;
    this.prev = null;
    this.next = null;
  }
}

export class LRUCache {
  #capacity;
  #index = new Map();
  // Sentinels: head.next is the most recently used, tail.prev the least.
  #head = new Node(null, null);
  #tail = new Node(null, null);

  constructor(capacity) {
    if (capacity <= 0) throw new RangeError('capacity must be > 0');
    this.#capacity = capacity;
    this.#head.next = this.#tail;
    this.#tail.prev = this.#head;
  }

  get(key) {
    const node = this.#index.get(key);
    if (!node) return undefined;
    this.#unlink(node);
    this.#pushFront(node);
    return node.value;
  }

  put(key, value) {
    let node = this.#index.get(key);
    if (node) {
      node.value = value;
      this.#unlink(node);
      this.#pushFront(node);
      return;
    }
    if (this.#index.size === this.#capacity) {
      const lru = this.#tail.prev;
      this.#unlink(lru);
      this.#index.delete(lru.key);
    }
    node = new Node(key, value);
    this.#index.set(key, node);
    this.#pushFront(node);
  }

  #pushFront(n) {
    n.prev = this.#head;
    n.next = this.#head.next;
    this.#head.next.prev = n;
    this.#head.next = n;
  }

  #unlink(n) {
    n.prev.next = n.next;
    n.next.prev = n.prev;
  }
}

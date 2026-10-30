// Single-threaded JS needs locks only when an operation spans an `await`.
class Mutex {
  #tail = Promise.resolve();
  async run(fn) {
    const prev = this.#tail;
    let release;
    this.#tail = new Promise((r) => (release = r));
    await prev;
    try { return await fn(); } finally { release(); }
  }
}

const inventoryLock = new Mutex();
export const reserve = (sku, qty) =>
  inventoryLock.run(async () => {
    const have = await stock.get(sku);       // without the mutex, two requests could both read the same value here
    if (have < qty) return false;
    await stock.set(sku, have - qty);
    return true;
  });

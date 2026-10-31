// An async mutex with a timeout (the idea behind libraries like async-mutex).
class Mutex {
  #locked = false;
  #waiters = [];
  async acquire(timeoutMs) {
    if (!this.#locked) { this.#locked = true; return true; }
    return new Promise((resolve) => {
      const waiter = (ok) => { clearTimeout(timer); resolve(ok); };
      const timer = setTimeout(() => { this.#waiters = this.#waiters.filter((w) => w !== waiter); resolve(false); }, timeoutMs);
      this.#waiters.push(waiter);
    });
  }
  release() {
    const next = this.#waiters.shift();
    if (next) next(true); else this.#locked = false;          // hand over directly to the next waiter
  }
}

// Break "no preemption": if the second lock isn't available soon, release the first and retry.
export async function transfer(from, to, amount, budgetMs = 1000) {
  const deadline = Date.now() + budgetMs;
  while (Date.now() < deadline) {
    if (await from.mutex.acquire(50)) {
      try {
        if (await to.mutex.acquire(50)) {
          try {
            from.balance -= amount;
            to.balance += amount;
            return true;
          } finally { to.mutex.release(); }
        }
      } finally { from.mutex.release(); }
    }
    await new Promise((r) => setTimeout(r, 1 + Math.random() * 19)); // RANDOM backoff — avoids livelock
  }
  return false;
}

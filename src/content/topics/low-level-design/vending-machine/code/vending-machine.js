const ACCEPTED = new Set([1, 2, 5, 10, 20]); // coin denominations (₹)

export class VendingMachine {
  #products = new Map(); // code -> { code, name, price }
  #stock = new Map();
  #coinBox = new Map(); // denomination -> count
  #inserted = [];
  state = 'IDLE'; // IDLE | HAS_CREDIT | OUT_OF_SERVICE

  insertCoin(coin) {
    if (this.state === 'OUT_OF_SERVICE' || !ACCEPTED.has(coin)) return false;
    this.#inserted.push(coin);
    this.state = 'HAS_CREDIT';
    return true;
  }

  select(code) {
    if (this.state !== 'HAS_CREDIT') return { message: 'Insert money first' };
    const p = this.#products.get(code);
    if (!p || !this.#stock.get(code)) return { message: 'Sold out' };
    const credit = this.credit();
    if (credit < p.price) return { message: `Insert ₹${p.price - credit} more` };

    const pool = new Map(this.#coinBox);
    for (const c of this.#inserted) pool.set(c, (pool.get(c) ?? 0) + 1);
    const change = makeChange(credit - p.price, pool);
    if (!change) return { message: 'Exact change only' };

    for (const c of this.#inserted) this.#coinBox.set(c, (this.#coinBox.get(c) ?? 0) + 1);
    const coinsOut = [];
    for (const [coin, n] of change) {
      this.#coinBox.set(coin, this.#coinBox.get(coin) - n);
      for (let i = 0; i < n; i++) coinsOut.push(coin);
    }
    this.#stock.set(code, this.#stock.get(code) - 1);
    this.#inserted = [];
    this.state = 'IDLE';
    return { product: p, change: coinsOut, message: `Enjoy your ${p.name}` };
  }

  cancel() {
    const refund = this.#inserted;
    this.#inserted = [];
    if (this.state === 'HAS_CREDIT') this.state = 'IDLE';
    return refund;
  }

  credit() {
    return this.#inserted.reduce((a, b) => a + b, 0);
  }

  addProduct(p, quantity) {
    this.#products.set(p.code, p);
    this.#stock.set(p.code, (this.#stock.get(p.code) ?? 0) + quantity);
  }
  loadCoins(coin, count) {
    this.#coinBox.set(coin, (this.#coinBox.get(coin) ?? 0) + count);
  }
  setOutOfService(off) {
    this.state = off ? 'OUT_OF_SERVICE' : 'IDLE';
  }
}

// Fewest coins for `amount` from a box with limited counts. Returns Map(coin -> n) or
// null.
export function makeChange(amount, box) {
  const coins = [...box.entries()];
  const INF = Number.MAX_SAFE_INTEGER;
  const table = (fill) => Array.from({ length: coins.length + 1 }, () => new Array(amount + 1).fill(fill));
  const best = table(INF);
  const take = table(0);
  best[0][0] = 0;
  coins.forEach(([coin, count], i) => {
    for (let a = 0; a <= amount; a++)
      for (let k = 0; k <= Math.min(count, Math.floor(a / coin)); k++)
        if (best[i][a - k * coin] + k < best[i + 1][a]) {
          best[i + 1][a] = best[i][a - k * coin] + k;
          take[i + 1][a] = k;
        }
  });
  if (best[coins.length][amount] === INF) return null;
  const change = new Map();
  for (let i = coins.length, a = amount; i > 0; i--) {
    const k = take[i][a];
    if (k) change.set(coins[i - 1][0], k);
    a -= k * coins[i - 1][0];
  }
  return change;
}

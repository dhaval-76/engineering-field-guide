// The cart stores intent (what and how many), not prices — prices are looked up when pricing.
export class Cart {
  #lines = new Map(); // sku -> qty, insertion order kept
  #coupons = new Set();
  version = 0; // optimistic locking when persisted

  constructor(cartId, ownerId) {
    Object.assign(this, { cartId, ownerId }); // ownerId: user id or a guest session id
  }

  add(sku, qty) {
    if (!(qty > 0)) throw new Error('qty must be > 0');
    this.#lines.set(sku, (this.#lines.get(sku) ?? 0) + qty);
  }

  setQuantity(sku, qty) {
    if (qty === 0) this.#lines.delete(sku);
    else if (qty > 0) this.#lines.set(sku, qty);
    else throw new Error('qty must be >= 0');
  }

  remove(sku) {
    this.#lines.delete(sku);
  }

  applyCoupon(code) {
    this.#coupons.add(code.toUpperCase());
  }

  // Guest cart merged into the user's cart at login: quantities add up.
  mergeFrom(guest) {
    for (const { sku, qty } of guest.lines()) this.add(sku, qty);
    guest.coupons().forEach((c) => this.#coupons.add(c));
  }

  lines() {
    return [...this.#lines].map(([sku, qty]) => ({ sku, qty }));
  }

  coupons() {
    return [...this.#coupons];
  }
}

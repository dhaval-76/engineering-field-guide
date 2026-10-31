// `checkoutId` comes from the client and makes the whole operation idempotent
// (double-clicks, retries).
export class CheckoutService {
  constructor({ carts, pricing, promotions, inventory, payments, orders }) {
    Object.assign(this, { carts, pricing, promotions, inventory, payments, orders });
  }

  async checkout(checkoutId, cartId, method) {
    const existing = await this.orders.findByCheckoutId(checkoutId);
    if (existing) return existing;

    const cart = await this.carts.get(cartId);
    // re-price: never trust the client
    const price = await this.pricing.price(cart, await this.promotions.activeFor(cart));

    // all-or-nothing
    const hold = await this.inventory.reserveAll(cart.lines(), { minutes: 15 });
    try {
      const paid = await this.payments.charge(price.payable, method, { idempotencyKey: checkoutId });
      if (!paid.succeeded) throw new Error(`payment declined: ${paid.reason}`);
      const order = await this.orders.save({ checkoutId, cartId, lines: cart.lines(), price, payment: paid.reference });
      await this.inventory.commit(hold);
      await this.carts.clear(cartId);
      return order;
    } catch (e) {
      await this.inventory.release(hold);
      throw e;
    }
  }
}

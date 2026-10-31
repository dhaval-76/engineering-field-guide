// Many subsystems, each with its own API and failure modes…
export class CheckoutFacade {
  constructor({ carts, pricing, inventory, payments, orders }) {
    Object.assign(this, { carts, pricing, inventory, payments, orders });
  }

  // …one simple entry point for controllers and other callers.
  async checkout(cartId, paymentDetails) {
    const cart = await this.carts.load(cartId);
    const quote = await this.pricing.quote(cart);
    const hold = await this.inventory.reserve(cart.items);
    try {
      const paid = await this.payments.pay({ amount: quote.total, ...paymentDetails });
      if (!paid.succeeded) throw new Error(`payment declined: ${paid.reason}`);
      const order = await this.orders.save({ cart, quote, payment: paid });
      await this.inventory.commit(hold);
      return { orderId: order.id, total: quote.total };
    } catch (e) {
      await this.inventory.release(hold); // the facade owns the clean-up rules
      throw e;
    }
  }
}

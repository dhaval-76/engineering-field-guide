// JavaScript has no `interface` keyword: the contract is the set of methods callers rely
// on (duck typing). Document it, and validate at the boundary if it matters.

/** @typedef {{ charge(amount: number, customerId: string): Promise<object>,
 * refund(txnId: string, amount: number): Promise<object> }} PaymentProcessor */

export class CardProcessor {
  async charge(amount, customerId) { /* call card gateway */ return { ok: true }; }
  async refund(txnId, amount) { return { ok: true }; }
}

export class UpiProcessor {
  async charge(amount, customerId) { /* call UPI switch */ return { ok: true }; }
  async refund(txnId, amount) { return { ok: true }; }
}

export class CheckoutService {
  /** @param {PaymentProcessor} payments */
  constructor(payments) {
    this.payments = payments;
  }

  async checkout(order) {
    return this.payments.charge(order.total, order.customerId);
  }
}

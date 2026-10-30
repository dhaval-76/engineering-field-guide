// A transition table is often the clearest "state pattern" in JS.
const transitions = {
  PLACED: { pay: 'PAID', cancel: 'CANCELLED' },
  PAID: { ship: 'SHIPPED', cancel: 'CANCELLED' },
  SHIPPED: { deliver: 'DELIVERED' },
  DELIVERED: {},
  CANCELLED: {},
};

export class Order {
  #state = 'PLACED';
  get state() { return this.#state; }

  #apply(action) {
    const next = transitions[this.#state][action];
    if (!next) throw new Error(`cannot ${action} when ${this.#state}`);
    this.#state = next;
  }
  pay() { this.#apply('pay'); }
  ship() { this.#apply('ship'); }
  deliver() { this.#apply('deliver'); }
  cancel() { this.#apply('cancel'); }
}

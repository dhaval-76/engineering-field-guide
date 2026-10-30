// The same idea without a framework: pass collaborators in, wire them once at startup.
export class CheckoutService {
  constructor({ gateway, orders }) {
    this.gateway = gateway;
    this.orders = orders;
  }
}

// composition root (main.js) — the only place that knows concrete classes
const orders = new PgOrderRepository(pool);
const gateway = new StripeGateway(process.env.STRIPE_KEY);
export const checkout = new CheckoutService({ gateway, orders });

// In tests: new CheckoutService({ gateway: fakeGateway, orders: inMemoryOrders })
// Frameworks such as NestJS (and libraries like awilix) automate this wiring with a container.

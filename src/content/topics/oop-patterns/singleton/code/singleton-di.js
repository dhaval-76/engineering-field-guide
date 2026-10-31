// Preferred in applications: create one instance at startup (the composition root) and
// pass it in.
export class InvoiceService {
  constructor({ clock }) {
    this.clock = clock; // dependency is visible in the constructor
  }
  issue(order) {
    return { order, issuedAt: this.clock.now() };
  }
}

// main.js — the only place that decides "one instance"
const clock = { now: () => new Date() };
const invoices = new InvoiceService({ clock });

// In a test, pass a fixed clock — no global state to reset:
const testService = new InvoiceService({ clock: { now: () => new Date('2026-01-01T00:00:00Z') } });
// (An ES module's top-level export is already a singleton — convenient, but harder to
// swap in tests.)

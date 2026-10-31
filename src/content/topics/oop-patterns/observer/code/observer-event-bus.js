// In-process event bus: handlers per event type, run asynchronously and isolated from
// each other.
export class EventBus {
  #handlers = new Map(); // type -> Set<handler>

  subscribe(type, handler) {
    if (!this.#handlers.has(type)) this.#handlers.set(type, new Set());
    this.#handlers.get(type).add(handler);
    return () => this.#handlers.get(type)?.delete(handler); // unsubscribe
  }

  publish(event) {
    for (const h of this.#handlers.get(event.type) ?? []) {
      queueMicrotask(async () => {            // don't block the publisher
        try {
          await h(event);
        } catch (err) {
          // one bad handler doesn't stop the others
          console.error(`handler failed for ${event.type}`, err);
        }
      });
    }
  }
}
// Node's built-in EventEmitter is synchronous: a throwing listener breaks the emit call.

// const bus = new EventBus(); const unsubscribe = bus.subscribe('OrderPlaced', (e) =>
// email.send(e)); bus.publish({ type: 'OrderPlaced', orderId: 'o-1', customerId: 'c-9',
// totalPaise: 1000 });

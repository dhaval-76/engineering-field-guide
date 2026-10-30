// JavaScript has proxies built in
function timed(target) {
  return new Proxy(target, {
    get(obj, prop, receiver) {
      const value = Reflect.get(obj, prop, receiver);
      if (typeof value !== 'function') return value;
      return async (...args) => {
        const start = performance.now();
        try {
          return await value.apply(obj, args);
        } finally {
          console.log(`${String(prop)} took ${(performance.now() - start).toFixed(1)} ms`);
        }
      };
    },
  });
}
const orders = timed(new OrderService(repo));
// NestJS interceptors and TypeScript decorators are the framework-level equivalents.

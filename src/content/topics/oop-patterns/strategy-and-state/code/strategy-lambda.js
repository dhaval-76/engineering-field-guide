// In JavaScript, a strategy is usually just a function.
const fixed = () => 200;
const exponential = (attempt) => 100 * 2 ** Math.min(attempt, 10);
const jittered = (attempt) => Math.floor(Math.random() * 100 * 2 ** Math.min(attempt, 10));

// retry(fetchOrders, { backoff: jittered })
// Built-in strategies are everywhere: Array.prototype.sort(compareFn), Promise.allSettled vs Promise.any, …
orders.sort((a, b) => b.createdAt - a.createdAt);

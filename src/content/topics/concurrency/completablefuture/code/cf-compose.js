// The CompletableFuture operations, in Promise terms.
const user = users.find(id);                                   // Promise<User>

// thenApply: transform the value (map)
const name = user.then((u) => u.displayName);
// thenCompose: next step is async (flatMap) — then() flattens
const orders = user.then((u) => orderClient.recent(u.id));
// thenCombine: join independent results
const quote = Promise.all([price, tax]).then(([p, t]) => ({ price: p, tax: t }));
// thenAccept: side effect, no new value
quote.then((q) => audit.log(q));
// anyOf: first SUCCESS wins (hedged requests)
const fastest = Promise.any([replicaA(), replicaB()]);
// first to settle, success or failure
const firstSettled = Promise.race([replicaA(), replicaB()]);
// There's no *Async variant: callbacks always run later on the event loop, never on a
// "completing thread".

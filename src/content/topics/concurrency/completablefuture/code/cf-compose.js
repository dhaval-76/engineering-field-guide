// The CompletableFuture operations, in Promise terms.
const user = users.find(id);                                   // Promise<User>

const name = user.then((u) => u.displayName);                  // thenApply: transform the value (map)
const orders = user.then((u) => orderClient.recent(u.id));     // thenCompose: next step is async (flatMap) — then() flattens
const quote = Promise.all([price, tax]).then(([p, t]) => ({ price: p, tax: t })); // thenCombine: join independent results
quote.then((q) => audit.log(q));                               // thenAccept: side effect, no new value
const fastest = Promise.any([replicaA(), replicaB()]);         // anyOf: first SUCCESS wins (hedged requests)
const firstSettled = Promise.race([replicaA(), replicaB()]);   // first to settle, success or failure
// There's no *Async variant: callbacks always run later on the event loop, never on a "completing thread".

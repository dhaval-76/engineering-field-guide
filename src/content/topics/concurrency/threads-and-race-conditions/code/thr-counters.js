// 1. Within one Node process a plain counter is safe: only one piece of JavaScript runs at a time.
let count = 0;
const increment = () => count++;

// 2. Across worker_threads, share memory explicitly and update it with atomic operations.
const shared = new Int32Array(new SharedArrayBuffer(4));
Atomics.add(shared, 0, 1);                        // atomic read-add-write, safe from any worker
Atomics.load(shared, 0);

// 3. Across processes or servers: keep the counter in Redis — INCR is atomic on the server.
await redis.incr('page:views');

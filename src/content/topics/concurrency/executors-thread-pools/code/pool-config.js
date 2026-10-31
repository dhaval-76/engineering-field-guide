// Node: limit concurrent async work with a small semaphore-style limiter (p-limit does
// this).
export function limit(concurrency) {
  const queue = [];
  let active = 0;
  const next = () => {
    if (active >= concurrency || queue.length === 0) return;
    active++;
    const { fn, resolve, reject } = queue.shift();
    fn().then(resolve, reject).finally(() => { active--; next(); });
  };
  return (fn) => new Promise((resolve, reject) => { queue.push({ fn, resolve, reject }); next(); });
}

const run = limit(10);
// at most 10 requests in flight
await Promise.all(urls.map((u) => run(() => fetch(u))));

// Node runs JavaScript on one thread, so `count++` can't be torn — but check-then-act ACROSS an await
// races exactly like threads do: other requests run while this one waits.
let balance = 1000;

async function withdraw(amount) {
  if (balance >= amount) {          // both calls see 1000 here…
    await recordAudit(amount);      // …because each yields here
    balance -= amount;              // …and both subtract → -800
  }
}

await Promise.all([withdraw(900), withdraw(900)]);
console.log(balance);               // -800

// Same bug with a cache: two concurrent misses both load
if (!cache.has(key)) cache.set(key, await load(key));
// ✓ store the promise synchronously, before awaiting, so the second caller reuses it
if (!cache.has(key)) cache.set(key, load(key));
const value = await cache.get(key);

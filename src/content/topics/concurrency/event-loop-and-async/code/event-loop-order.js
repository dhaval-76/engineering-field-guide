console.log('1 sync');

setTimeout(() => console.log('5 timer (macrotask)'), 0);

Promise.resolve().then(() => console.log('3 promise (microtask)'));

queueMicrotask(() => console.log('4 microtask'));

(async () => {
  console.log('2 async fn runs synchronously until its first await');
  await null;
  console.log('4b after await (microtask)');
})();

// Output: 1 sync, 2 async fn…, 3 promise, 4 microtask, 4b after await, 5 timer Rule: run
// the current script to the end, drain ALL microtasks, then take ONE macrotask, repeat.

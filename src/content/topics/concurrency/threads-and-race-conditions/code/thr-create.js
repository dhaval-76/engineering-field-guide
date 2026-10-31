// Node runs your JavaScript on ONE thread (the event loop). Concurrency comes from async
// I/O. Real parallelism for CPU work uses worker threads, which don't share ordinary
// objects:
import { Worker } from 'node:worker_threads';
const w = new Worker(new URL('./hash-worker.js', import.meta.url), {
  workerData: { file: 'big.bin' },
});
w.on('message', (digest) => console.log(digest));
// Shared memory exists only via SharedArrayBuffer + Atomics — the same race rules then
// apply.

// JavaScript has one thread per event loop, so nothing "sleeps" — you await a timer or a promise.
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
await sleep(200);                                  // other callbacks keep running meanwhile

// "join" a worker thread
import { Worker } from 'node:worker_threads';
const worker = new Worker('./report.js');
await new Promise((resolve) => worker.once('exit', resolve));

// "wait for a condition": resolve a promise when the condition becomes true
let markReady;
const ready = new Promise((resolve) => (markReady = resolve));
// producer: markReady(data);   consumer: const data = await ready;

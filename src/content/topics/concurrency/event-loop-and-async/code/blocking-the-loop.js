// ✗ CPU-heavy work on the event loop: every other request waits for it.
app.get('/report', (req, res) => {
  const result = crunchNumbers(hugeDataset);     // 2 seconds of CPU → whole server frozen for 2 s
  res.json(result);
});

// ✓ Move CPU work to a worker thread (or a separate service / queue).
import { Worker } from 'node:worker_threads';
app.get('/report', async (req, res) => {
  const result = await new Promise((resolve, reject) => {
    const w = new Worker(new URL('./crunch.js', import.meta.url), { workerData: req.query });
    w.once('message', resolve);
    w.once('error', reject);
  });
  res.json(result);
});

// ✓ I/O is fine: fetch, DB drivers and fs.promises don't block the loop while waiting.
// Also avoid: fs.readFileSync, JSON.parse on huge payloads, catastrophic regexes, sync crypto.

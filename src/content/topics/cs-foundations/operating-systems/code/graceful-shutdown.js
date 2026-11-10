const inFlight = new Set();
let accepting = true;

function startJob() {
  if (!accepting) return; // during shutdown, take no new work
  const job = new Promise((resolve) => setTimeout(resolve, 500)).then(() => {
    console.log('job done');
    inFlight.delete(job);
  });
  inFlight.add(job);
}

// Without a handler, Node exits at once on SIGTERM. Nothing can catch SIGKILL.
process.on('SIGTERM', async () => {
  console.log('SIGTERM: draining');
  accepting = false; // a server would also call server.close() here
  const deadline = new Promise((resolve) => setTimeout(resolve, 20_000).unref());
  await Promise.race([Promise.all(inFlight), deadline]);
  console.log('drained');
  process.exit(0);
});

for (let i = 0; i < 3; i++) startJob();
setInterval(() => {}, 1 << 30); // serve until a signal arrives

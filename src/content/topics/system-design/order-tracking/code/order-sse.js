// Express SSE: catch up from history after Last-Event-ID, then stream live changes from
// Redis pub/sub.
app.get('/orders/:id/events', requireAuth, async (req, res) => {
  const id = Number(req.params.id);
  await orders.requireOwner(id, req.user);
  res.writeHead(200, { 'content-type': 'text/event-stream', 'cache-control': 'no-cache', connection: 'keep-alive' });
  const send = (s) => res.write(`id: ${s.sequence}\nevent: status\ndata: ${JSON.stringify(s)}\n\n`);

  // a subscribed connection can't run other commands
  const sub = redis.duplicate();
  const buffered = [];
  let caughtUp = false;
  // subscribe FIRST so nothing slips between history and live
  await sub.subscribe(`order:${id}`);
  sub.on('message', (_, msg) => (caughtUp ? send(JSON.parse(msg)) : buffered.push(JSON.parse(msg))));

  const lastSeen = Number(req.get('Last-Event-ID') ?? 0);
  const history = await orders.historyAfter(id, lastSeen);
  history.forEach(send);
  const maxSeq = history.at(-1)?.sequence ?? lastSeen;
  buffered.filter((s) => s.sequence > maxSeq).forEach(send); // de-duplicate the overlap
  caughtUp = true;

  const ping = setInterval(() => res.write(': ping\n\n'), 25_000);
  req.on('close', () => { clearInterval(ping); sub.quit(); });
});

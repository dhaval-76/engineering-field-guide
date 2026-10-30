// Express: one SSE stream per order; replays missed events using Last-Event-ID.
app.get('/orders/:id/events', async (req, res) => {
  const { id } = req.params;
  await orders.checkCanRead(req.user, id); // authorize once, at connect

  res.writeHead(200, {
    'content-type': 'text/event-stream',
    'cache-control': 'no-cache',
    connection: 'keep-alive',
  });
  const send = (e) => res.write(`id: ${e.id}\nevent: status\ndata: ${JSON.stringify(e)}\n\n`);

  for (const e of await eventStore.eventsAfter(id, req.get('Last-Event-ID'))) send(e); // catch up
  const unsubscribe = eventBus.subscribe(`order:${id}`, send); // live events from pub/sub
  const heartbeat = setInterval(() => res.write(': ping\n\n'), 15_000);

  req.on('close', () => {
    clearInterval(heartbeat);
    unsubscribe();
  });
});

// Browser: EventSource reconnects automatically and sends Last-Event-ID for you.
const source = new EventSource('/orders/A-1001/events', { withCredentials: true });
source.addEventListener('status', (msg) => render(JSON.parse(msg.data)));

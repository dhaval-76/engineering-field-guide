// Express receiver: verify, de-duplicate, acknowledge fast, process asynchronously.
// express.raw keeps the exact bytes — re-serialised JSON won't match the signature.
app.post('/webhooks/payments', express.raw({ type: 'application/json' }), async (req, res) => {
  const body = req.body.toString('utf8');
  if (!verify(body, req.get('Webhook-Signature') ?? '', secret, Math.floor(Date.now() / 1000), 300)) {
    return res.status(401).end();
  }
  const eventId = req.get('Webhook-Id');
  const { rowCount } = await pool.query(
    'INSERT INTO webhook_inbox (event_id, body) VALUES ($1, $2) ON CONFLICT DO NOTHING', [eventId, body]);
  // duplicates are acknowledged, not re-processed
  if (rowCount === 1) await queue.add('process-webhook', { eventId });
  // answer within seconds; heavy work later
  res.status(200).end();
});

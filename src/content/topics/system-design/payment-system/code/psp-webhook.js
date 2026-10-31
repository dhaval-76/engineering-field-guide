// Charging: an unknown outcome (timeout) is never "failed" and never retried with a new
// key.
export async function charge(payments, psp, paymentId) {
  const p = await payments.get(paymentId);
  try {
    const r = await psp.capture(p.pspToken, p.amount, p.currency, { idempotencyKey: p.id, signal: AbortSignal.timeout(10_000) });
    return payments.transition(p.id, r.succeeded ? 'CAPTURED' : 'DECLINED', r.reference);
  } catch (e) {
    if (!(e.name === 'TimeoutError' || e.status >= 500)) throw e;
    await payments.transition(p.id, 'PENDING_CONFIRMATION', null);
    // query the PSP by our key if no webhook arrives
    await confirmations.scheduleLookup(p.id, { seconds: 30 });
    return payments.get(p.id);
  }
}

// Webhook: verify, de-duplicate, apply a valid transition — all in one transaction.
app.post('/webhooks/psp', express.raw({ type: 'application/json' }), async (req, res) => {
  // HMAC over raw body + timestamp tolerance
  const e = verifyAndParse(req.get('X-Signature'), req.body);
  if (!e) return res.status(400).end();
  await inTx(pool, async (c) => {
    const fresh = await c.query('INSERT INTO processed_psp_events (event_id) VALUES ($1) ON CONFLICT DO NOTHING', [e.id]);
    if (fresh.rowCount === 0) return;                          // duplicate delivery
    // + ledger entries + outbox row
    await paymentStateMachine.apply(c, e.paymentId, e.type, e.amount, e.reference);
  });
  // 2xx tells the PSP to stop retrying
  res.status(200).end();
});

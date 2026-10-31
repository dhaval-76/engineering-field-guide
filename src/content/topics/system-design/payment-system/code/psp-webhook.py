# Charging: an unknown outcome (timeout) is never "failed" and never retried with a new key.
import httpx

def charge(payments, psp, payment_id):
    p = payments.get(payment_id)
    try:
        r = psp.capture(p.psp_token, p.amount, p.currency, idempotency_key=str(p.id), timeout=10)
        return payments.transition(p.id, "CAPTURED" if r.succeeded else "DECLINED", r.reference)
    except (httpx.TimeoutException, httpx.HTTPStatusError) as e:
        if isinstance(e, httpx.HTTPStatusError) and e.response.status_code < 500:
            raise
        payments.transition(p.id, "PENDING_CONFIRMATION", None)
        confirmations.schedule_lookup(p.id, seconds=30)      # query the PSP by our key if no webhook arrives
        return payments.get(p.id)

# Webhook (FastAPI): verify, de-duplicate, apply a valid transition — all in one transaction.
@app.post("/webhooks/psp")
async def psp_webhook(request: Request, x_signature: str = Header(...)):
    e = verify_and_parse(x_signature, await request.body())   # HMAC over raw body + timestamp tolerance
    if e is None:
        raise HTTPException(400)
    async with pool.connection() as conn, conn.transaction():
        fresh = await conn.execute("INSERT INTO processed_psp_events (event_id) VALUES (%s) ON CONFLICT DO NOTHING",
                                   (e.id,))
        if fresh.rowcount == 1:                                # duplicate deliveries do nothing
            await payment_state_machine.apply(conn, e.payment_id, e.type, e.amount, e.reference)
    return {"ok": True}                                        # 2xx tells the PSP to stop retrying

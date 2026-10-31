# FastAPI receiver: verify, de-duplicate, acknowledge fast, process asynchronously.
import time
from fastapi import Header, HTTPException, Request

@app.post("/webhooks/payments")
async def receive(request: Request, webhook_signature: str = Header(...), webhook_id: str = Header(...)):
    # raw bytes: re-serialised JSON won't match the signature
    body = (await request.body()).decode()
    if not verify(body, webhook_signature, SECRET, int(time.time()), 300):
        raise HTTPException(401)
    async with pool.connection() as conn:
        cur = await conn.execute("INSERT INTO webhook_inbox (event_id, body) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                                 (webhook_id, body))
    if cur.rowcount == 1:
        # duplicates are acknowledged, not re-processed
        await queue.enqueue("process_webhook", webhook_id)
    # answer within seconds; heavy work later
    return {"ok": True}

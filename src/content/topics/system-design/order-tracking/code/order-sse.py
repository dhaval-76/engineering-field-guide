# FastAPI SSE: catch up from history after Last-Event-ID, then stream live changes from Redis pub/sub.
import asyncio, json
from fastapi import Depends, Header, Request
from fastapi.responses import StreamingResponse

@app.get("/orders/{order_id}/events")
async def order_events(order_id: int, request: Request, last_event_id: int = Header(0), user=Depends(current_user)):
    await orders.require_owner(order_id, user)

    async def stream():
        pubsub = redis.pubsub()
        await pubsub.subscribe(f"order:{order_id}")              # subscribe FIRST: nothing slips between history and live
        try:
            max_seq = last_event_id
            for s in await orders.history_after(order_id, last_event_id):
                max_seq = s["sequence"]
                yield f"id: {s['sequence']}\nevent: status\ndata: {json.dumps(s)}\n\n"
            while not await request.is_disconnected():
                msg = await pubsub.get_message(ignore_subscribe_messages=True, timeout=25)
                if msg is None:
                    yield ": ping\n\n"                           # keep proxies from closing idle streams
                    continue
                s = json.loads(msg["data"])
                if s["sequence"] > max_seq:                      # de-duplicate the history/live overlap
                    max_seq = s["sequence"]
                    yield f"id: {s['sequence']}\nevent: status\ndata: {json.dumps(s)}\n\n"
        finally:
            await pubsub.unsubscribe()

    return StreamingResponse(stream(), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})

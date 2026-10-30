# FastAPI: one SSE stream per order; replays missed events using Last-Event-ID.
import asyncio, json
from fastapi import Depends, FastAPI, Header, Request
from fastapi.responses import StreamingResponse

app = FastAPI()

def fmt(e: dict) -> str:
    return f"id: {e['id']}\nevent: status\ndata: {json.dumps(e)}\n\n"

@app.get("/orders/{order_id}/events")
async def order_events(order_id: str, request: Request,
                       last_event_id: str | None = Header(default=None),   # "Last-Event-ID"
                       user=Depends(current_user)):
    await orders.check_can_read(user, order_id)           # authorize once, at connect

    async def stream():
        for e in await event_store.events_after(order_id, last_event_id):   # catch up
            yield fmt(e)
        async with event_bus.subscribe(f"order:{order_id}") as queue:        # live events
            while not await request.is_disconnected():
                try:
                    yield fmt(await asyncio.wait_for(queue.get(), timeout=15))
                except asyncio.TimeoutError:
                    yield ": ping\n\n"                     # heartbeat keeps proxies from closing it

    return StreamingResponse(stream(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache"})

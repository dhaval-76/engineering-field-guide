# FastAPI WebSockets + Redis pub/sub: any instance can deliver to any room.
import asyncio, json
from collections import defaultdict
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
import redis.asyncio as redis

app = FastAPI()
r = redis.from_url("redis://localhost:6379")
rooms: dict[str, set[WebSocket]] = defaultdict(set)

async def relay():
    pubsub = r.pubsub()
    await pubsub.psubscribe("room:*")
    async for msg in pubsub.listen():
        if msg["type"] != "pmessage":
            continue
        room, payload = msg["channel"].decode(), msg["data"].decode()
        for ws in list(rooms[room]):
            try:
                await asyncio.wait_for(ws.send_text(payload), timeout=5)   # slow client: give up
            except Exception:
                rooms[room].discard(ws)

@app.on_event("startup")
async def start_relay():
    asyncio.create_task(relay())

@app.websocket("/ws")
async def socket(ws: WebSocket):
    await ws.accept()
    joined: set[str] = set()
    try:
        while True:
            m = json.loads(await ws.receive_text())
            room = f"room:{m['room']}"
            if m["type"] == "join":
                rooms[room].add(ws)
                joined.add(room)
            elif m["type"] == "say":
                await r.publish(room, json.dumps({"room": m["room"], "text": m["text"]}))
    except WebSocketDisconnect:
        for room in joined:
            rooms[room].discard(ws)

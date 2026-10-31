# Two cache levels in front of the database.
import json
from cachetools import TTLCache
from fastapi import Header, HTTPException, Response

local = TTLCache(maxsize=50_000, ttl=30)          # Level 1: in-process — microseconds, short TTL

def get_product(product_id: int) -> dict:
    if (v := local.get(product_id)) is not None:
        return v
    cached = redis.get(f"product:v1:{product_id}")           # Level 2: shared across instances
    if cached:
        v = json.loads(cached)
    else:
        v = repo.find_view(product_id)
        if v is None:
            raise HTTPException(404)
        redis.set(f"product:v1:{product_id}", json.dumps(v), ex=600)
    local[product_id] = v
    return v

# Shed load early, keep the request path fast; slow work goes to consumers of the outbox event.
@app.post("/orders", status_code=201)
def place(cmd: PlaceOrder, response: Response, x_api_key: str = Header(...)):
    if not per_client.try_acquire(x_api_key):
        raise HTTPException(429, headers={"Retry-After": "1"})
    return orders.place(cmd)                                 # DB transaction + outbox row only

# Worker (separate process): retried independently; never slows checkout.
def on_order_placed(event: dict) -> None:
    emails.send_confirmation(event)

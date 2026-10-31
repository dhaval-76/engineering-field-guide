import asyncio

async def summarize(order, pricing, inventory) -> list[dict]:
    """Enrich every item in parallel; one bad item never fails the whole response."""
    async def enrich(item) -> dict:
        try:
            price, in_stock = await asyncio.wait_for(asyncio.gather(
                pricing.price_of(item.sku), inventory.in_stock(item.sku, item.qty)), timeout=0.3)
            if in_stock:
                return {"item": item, "price": price, "status": "AVAILABLE"}
        except Exception:                               # timeout or error from either call
            pass
        return {"item": item, "price": None, "status": "UNAVAILABLE"}

    return await asyncio.gather(*(enrich(i) for i in order.items))

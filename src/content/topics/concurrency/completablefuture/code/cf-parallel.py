# asyncio: gather runs the coroutines concurrently; wait_for adds a deadline.
async def build_page(product_id: str) -> ProductPage:
    async def stock_or_unknown():
        try:
            return await asyncio.wait_for(inventory_service.get(product_id), 0.3)
        except Exception:
            return Inventory.UNKNOWN

    product, stock, price = await asyncio.gather(
        asyncio.wait_for(product_service.get(product_id), 0.3),
        stock_or_unknown(),
        asyncio.wait_for(pricing_service.get(product_id), 0.3),
    )
    return ProductPage(product, stock, price)

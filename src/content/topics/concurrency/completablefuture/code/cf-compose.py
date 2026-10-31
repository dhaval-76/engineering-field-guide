# The CompletableFuture operations, in asyncio terms.
import asyncio

async def examples(user_id: str):
    user = await users.find(user_id)                         # supplyAsync + join
    # thenApply: just ordinary code after await
    name = user.display_name
    # thenCompose: await the next async step
    orders = await order_client.recent(user.id)

    # thenCombine / allOf
    price, tax = await asyncio.gather(pricing.get(user_id), tax_service.get(user_id))

    # anyOf: first to finish wins (hedged requests); cancel the loser
    a, b = asyncio.create_task(replica_a()), asyncio.create_task(replica_b())
    done, pending = await asyncio.wait({a, b}, return_when=asyncio.FIRST_COMPLETED)
    for t in pending:
        t.cancel()
    fastest = done.pop().result()

    # CPU-heavy or blocking work: run it off the event loop (the *Async-with-executor
    # variant)
    enriched = await asyncio.to_thread(slow_enrichment, user)
    return name, orders, price, tax, fastest, enriched

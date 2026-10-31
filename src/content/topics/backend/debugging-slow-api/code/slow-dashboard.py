import asyncio

# BEFORE: four independent calls in sequence → 4 × 750 ms = 3 s.  AFTER: in parallel →
# ~750 ms.
async def load_dashboard(user_id: str) -> dict:
    async def with_timeout(coro, seconds=1.0):
        return await asyncio.wait_for(coro, seconds)

    user, recent, unread, balance = await asyncio.gather(
        with_timeout(users.get(user_id)),
        with_timeout(orders.recent(user_id)),
        with_timeout(notifications.unread_count(user_id)),
        with_timeout(accounts.balance(user_id)),
        # one failure doesn't cancel the others
        return_exceptions=True,
    )
    if isinstance(user, Exception) or isinstance(balance, Exception):
        raise RuntimeError("dashboard unavailable")   # essential parts failed
    return {
        "user": user,
        "balance": balance,
        # optional parts degrade gracefully
        "recent": [] if isinstance(recent, Exception) else recent,
        "unread": None if isinstance(unread, Exception) else unread,
    }

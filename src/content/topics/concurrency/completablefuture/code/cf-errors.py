import asyncio
import logging

log = logging.getLogger(__name__)

async def price_or_fallback(item_id: str) -> dict:
    # orTimeout + exceptionally: recover from a failure with a fallback value
    try:
        # cancels the call on timeout
        return await asyncio.wait_for(pricing.get(item_id), timeout=0.2)
    except (asyncio.TimeoutError, PricingError):
        return {"unavailable": True}

async def price_logged(item_id: str) -> dict:
    # handle + whenComplete: see the outcome, log it, record metrics, return a value
    # either way
    try:
        return await pricing.get(item_id)
    except Exception:
        log.warning("pricing failed for %s", item_id, exc_info=True)
        return {"unavailable": True}
    finally:
        metrics.increment("pricing.calls")
# An exception propagates through every following `await` until something catches it.

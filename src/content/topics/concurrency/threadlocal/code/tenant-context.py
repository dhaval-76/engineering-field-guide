# contextvars: per-request context that works with threads AND asyncio tasks (Python's
# ThreadLocal).
import contextvars
import logging
from fastapi import Depends, HTTPException

tenant_id: contextvars.ContextVar[str] = contextvars.ContextVar("tenant_id")

async def tenant_scope(claims: dict = Depends(current_user)):
    # Take the tenant from the authenticated token's claims, never from a header the
    # client controls.
    if not claims.get("org_id"):
        raise HTTPException(403)
    token = tenant_id.set(claims["org_id"])
    try:
        yield claims["org_id"]
    finally:
        tenant_id.reset(token)                 # cleared even on errors

# every log line gets the tenant: "%(tenant)s %(message)s"
class TenantFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.tenant = tenant_id.get("-")
        return True

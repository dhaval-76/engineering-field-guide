# contextvars work for both threads and asyncio tasks (each task gets a copy of the
# context).
from contextvars import ContextVar

trace_id: ContextVar[str | None] = ContextVar("trace_id", default=None)

async def middleware(request, call_next):
    token = trace_id.set(request.headers.get("x-trace-id"))
    try:
        return await call_next(request)
    finally:
        trace_id.reset(token)

# anywhere: log.info("[%s] refund issued", trace_id.get())
# threading.local() is the older, thread-only equivalent.

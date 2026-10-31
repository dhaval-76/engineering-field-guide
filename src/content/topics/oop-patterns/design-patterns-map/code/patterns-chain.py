from typing import Callable

Handler = Callable[[dict, Callable[[dict], dict]], dict]   # (request, next) -> response

def pipeline(handlers: list[Handler], endpoint: Callable[[dict], dict]) -> Callable[[dict], dict]:
    """Chain of Responsibility: each handler handles, rejects, or calls next() — like WSGI/ASGI middleware."""
    def run(i: int, req: dict) -> dict:
        return endpoint(req) if i == len(handlers) else handlers[i](req, lambda r: run(i + 1, r))
    return lambda req: run(0, req)

def auth(req, nxt):
    return nxt(req) if req.get("valid_token") else {"status": 401}

def rate_limit(req, nxt):
    return nxt(req) if limiter.try_acquire(req["client_id"]) else {"status": 429}

# pipeline([auth, rate_limit], controller.handle)(request)

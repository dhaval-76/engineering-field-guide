# redis-py. Opaque random session ids; reads slide the expiry atomically.
import json
import secrets

TTL_SECONDS = 30 * 60

def _key(session_id: str) -> str:
    return f"session:{session_id}"

def create_session(r, data: dict) -> str:
    session_id = secrets.token_urlsafe(32)                # unguessable id
    r.set(_key(session_id), json.dumps(data), ex=TTL_SECONDS)
    return session_id

def touch_session(r, session_id: str) -> dict | None:
    # read and slide the expiry atomically (Redis 6.2+)
    raw = r.getex(_key(session_id), ex=TTL_SECONDS)
    return None if raw is None else json.loads(raw)

def invalidate_session(r, session_id: str) -> None:       # logout
    r.delete(_key(session_id))

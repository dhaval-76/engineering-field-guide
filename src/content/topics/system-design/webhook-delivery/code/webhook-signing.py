import hashlib
import hmac
import random

def _hmac_hex(data: str, secret: bytes) -> str:
    return hmac.new(secret, data.encode(), hashlib.sha256).hexdigest()

def signature_header(body: str, timestamp: int, secret: bytes) -> str:
    """Sender: sign "timestamp.body"."""
    return f"t={timestamp},v1={_hmac_hex(f'{timestamp}.{body}', secret)}"

def verify(body: str, header: str, secret: bytes, now: int, tolerance_seconds: int = 300) -> bool:
    """Receiver: verify the RAW body, constant-time compare, reject stale timestamps (replays)."""
    parts = dict(p.split("=", 1) for p in header.split(",") if "=" in p)
    try:
        t = int(parts["t"])
        v1 = parts["v1"]
    except (KeyError, ValueError):
        return False
    if abs(now - t) > tolerance_seconds:
        return False
    return hmac.compare_digest(_hmac_hex(f"{t}.{body}", secret), v1)

def next_delay_seconds(attempt: int, rnd: random.Random = random.Random()) -> int:
    """Exponential backoff with jitter, capped at 6 hours between attempts."""
    base = min(6 * 3600, 30 * 2 ** min(attempt, 20))
    return int(base / 2 + rnd.random() * base / 2)

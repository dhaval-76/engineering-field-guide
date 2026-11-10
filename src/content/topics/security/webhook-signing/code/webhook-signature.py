import hashlib
import hmac

TOLERANCE_SECONDS = 300  # reject anything older: a replay


def signature_header(secret: str, timestamp: int, body: str) -> str:
    """Stripe-style header "t=<unix seconds>,v1=<hex HMAC-SHA256 of t.body>"."""
    return f"t={timestamp},v1={hmac_hex(secret, f'{timestamp}.{body}')}"


def verify(secret: str, header: str, body: str, now_seconds: int) -> bool:
    """body must be the raw request body exactly as received, not re-serialised JSON."""
    timestamp, signatures = -1, []
    for part in header.split(","):
        key, _, value = part.strip().partition("=")
        if key == "t" and value.isdigit():
            timestamp = int(value)
        elif key == "v1":
            signatures.append(value)  # ignore other schemes
    if timestamp < 0 or abs(now_seconds - timestamp) > TOLERANCE_SECONDS:
        return False
    expected = hmac_hex(secret, f"{timestamp}.{body}")
    # constant time; several signatures during a secret rotation
    return any(hmac.compare_digest(expected, s) for s in signatures)


def hmac_hex(secret: str, message: str) -> str:
    return hmac.new(secret.encode(), message.encode(), hashlib.sha256).hexdigest()

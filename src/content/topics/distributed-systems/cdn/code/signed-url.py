import base64
import hashlib
import hmac

def sign(path: str, expires_epoch_seconds: int, secret: bytes) -> str:
    """Signed URLs let the CDN serve private files (paid videos, invoices) without calling the origin."""
    msg = f"{path}?expires={expires_epoch_seconds}".encode()
    sig = base64.urlsafe_b64encode(hmac.new(secret, msg, hashlib.sha256).digest()).rstrip(b"=").decode()
    return f"{path}?expires={expires_epoch_seconds}&sig={sig}"

def verify(path: str, expires: int, sig: str, secret: bytes, now_epoch_seconds: int) -> bool:
    """Runs at the edge: reject expired or tampered links."""
    if now_epoch_seconds > expires:
        return False
    expected = sign(path, expires, secret)
    # constant time
    return hmac.compare_digest(expected, f"{path}?expires={expires}&sig={sig}")

import base64
import hashlib
import hmac
import os

ITERATIONS = 600_000  # OWASP's minimum for PBKDF2-HMAC-SHA256


def hash_password(password: str) -> str:
    """Returns "pbkdf2_sha256$iterations$salt$hash": everything needed to check it later."""
    salt = os.urandom(16)  # unique per password, so equal passwords differ
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, ITERATIONS)
    b64 = lambda b: base64.b64encode(b).decode()
    return f"pbkdf2_sha256${ITERATIONS}${b64(salt)}${b64(digest)}"


def verify_password(password: str, stored: str) -> bool:
    _, iterations, salt, expected = stored.split("$")
    actual = hashlib.pbkdf2_hmac(
        "sha256", password.encode(), base64.b64decode(salt), int(iterations)
    )
    return hmac.compare_digest(actual, base64.b64decode(expected))  # constant time

# Store password HASHES from a slow, salted algorithm — never plain SHA-256, never
# encryption. Standard-library scrypt; argon2-cffi (argon2id) is an equally good choice.
import base64
import hashlib
import hmac
import os
import unicodedata

N, R, P = 2 ** 15, 8, 1
MAXMEM = 64 * 1024 * 1024

def hash_password(password: str) -> str:
    salt = os.urandom(16)
    h = hashlib.scrypt(unicodedata.normalize("NFKC", password).encode(),
                       salt=salt, n=N, r=R, p=P, maxmem=MAXMEM, dklen=32)
    b64 = lambda b: base64.b64encode(b).decode()
    # params stored → upgradable later
    return f"scrypt${N}${R}${P}${b64(salt)}${b64(h)}"

def verify_password(password: str, stored: str) -> bool:
    _, n, r, p, salt, h = stored.split("$")
    expected = base64.b64decode(h)
    actual = hashlib.scrypt(unicodedata.normalize("NFKC", password).encode(), salt=base64.b64decode(salt),
                            n=int(n), r=int(r), p=int(p), maxmem=MAXMEM, dklen=len(expected))
    # constant-time comparison
    return hmac.compare_digest(actual, expected)

DUMMY_HASH = hash_password("not-a-real-password")

def login(users, email: str, password: str):
    u = users.find_by_email(email)
    # same work when the user doesn't exist
    ok = verify_password(password, u.password_hash if u else DUMMY_HASH)
    # one error message for "no such user" and "wrong password"
    return u if ok and u else None
# Plus: rate-limit and lock out per account and per IP, check breached-password lists,
# offer MFA / passkeys.

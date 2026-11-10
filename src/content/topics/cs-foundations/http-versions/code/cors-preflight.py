import re

# Request headers a page may send cross-origin without a preflight (Fetch standard).
SAFELISTED = {"accept", "accept-language", "content-language", "content-type", "range"}
SIMPLE_TYPES = {"application/x-www-form-urlencoded", "multipart/form-data", "text/plain"}


def needs_preflight(method: str, headers: dict[str, str]) -> bool:
    """Does a browser send an OPTIONS preflight before this cross-origin fetch?"""
    if method not in ("GET", "HEAD", "POST"):
        return True
    for key, value in headers.items():
        name = key.lower()
        if name not in SAFELISTED:
            return True  # e.g. Authorization, X-Request-Id
        if name == "content-type":
            if value.split(";")[0].strip().lower() not in SIMPLE_TYPES:
                return True  # e.g. application/json
        if name == "range" and not re.fullmatch(r"bytes=\d+-\d*", value):
            return True
    return False

import hashlib


def serve(body: str, if_none_match: str | None) -> tuple[int, dict[str, str], str]:
    """Serves a cacheable resource, answering 304 Not Modified when the client's copy is current."""
    etag = '"' + hashlib.sha256(body.encode()).hexdigest()[:16] + '"'
    headers = {
        "ETag": etag,
        # reuse for 60 s without asking; afterwards revalidate with If-None-Match
        "Cache-Control": "max-age=60",
    }
    if if_none_match and matches(if_none_match, etag):
        return 304, headers, ""  # no body: the client already has it
    return 200, headers, body


def matches(if_none_match: str, etag: str) -> bool:
    """If-None-Match may list several tags, or "*"; W/ (weak) tags compare by value."""
    for tag in (t.strip() for t in if_none_match.split(",")):
        if tag == "*" or tag.removeprefix("W/") == etag:
            return True
    return False

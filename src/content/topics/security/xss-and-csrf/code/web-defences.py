import hmac
from html import escape


def escape_html(text: str) -> str:
    """Encodes text for an HTML body or a quoted attribute, so it can never become markup."""
    return escape(text, quote=True)  # & < > " ' become entities


def allow_state_change(method, headers, our_origin, session_token, sent_token) -> bool:
    """CSRF check for a state-changing request. The browser says where the request came from
    (Sec-Fetch-Site, or Origin in older browsers), and the form must echo the session's token.
    Header names are lower-case here."""
    if method in ("GET", "HEAD", "OPTIONS"):
        return True  # must change nothing
    site = headers.get("sec-fetch-site")
    if site is not None:
        if site not in ("same-origin", "none"):
            return False  # another site
    elif headers.get("origin") != our_origin:
        return False
    return sent_token is not None and hmac.compare_digest(session_token, sent_token)

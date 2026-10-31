# Refresh tokens: random (not JWT), stored HASHED, single-use, rotated, grouped into a
# family per login.
import hashlib
import secrets
import uuid
from datetime import datetime, timedelta, timezone

class InvalidGrant(Exception):
    pass

def sha256(s: str) -> str:
    return hashlib.sha256(s.encode()).hexdigest()

def refresh(repo, tokens, presented: str, now: datetime | None = None) -> dict:
    """repo: find_by_token_hash, mark_used_if_unused, revoke_family, insert (a refresh_tokens table)."""
    now = now or datetime.now(timezone.utc)
    rt = repo.find_by_token_hash(sha256(presented))
    if rt is None or rt.revoked_at or rt.expires_at <= now:
        raise InvalidGrant()

    # UPDATE refresh_tokens SET used_at = :now WHERE id = :id AND used_at IS NULL  → rows
    # changed
    if repo.mark_used_if_unused(rt.id, now) == 0:
        # reuse of an old token ⇒ possible theft: end that login everywhere
        repo.revoke_family(rt.family_id)
        raise InvalidGrant()

    nxt = secrets.token_urlsafe(32)
    repo.insert(dict(id=uuid.uuid4(), user_id=rt.user_id, family_id=rt.family_id, token_hash=sha256(nxt),
                     expires_at=now + timedelta(days=14), used_at=None, revoked_at=None))
    return {"access_token": tokens.issue_access_token(rt.user_id, timedelta(minutes=10)), "refresh_token": nxt}

def logout(repo, presented: str) -> None:
    rt = repo.find_by_token_hash(sha256(presented))
    if rt is not None:
        repo.revoke_family(rt.family_id)

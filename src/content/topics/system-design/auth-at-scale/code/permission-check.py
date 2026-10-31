# Relationship tuples (Zanzibar-style), e.g. in OpenFGA / SpiceDB:
#   folder:roadmap#editor@user:asha
#   document:q3-plan#parent@folder:roadmap
# Model: a document's editors include editors of its parent folder.
from cachetools import TTLCache

class AccessDenied(Exception):
    pass

class DocumentAccess:
    def __init__(self, permissions):
        self.permissions = permissions                    # OpenFGA/SpiceDB client, a replicated service
        self._recent = TTLCache(maxsize=100_000, ttl=5)   # short TTL: revocations take effect within seconds

    def require_edit(self, user_id: str, doc_id: str) -> None:
        key = (user_id, "edit", doc_id)
        allowed = self._recent.get(key)
        if allowed is None:
            allowed = self._recent[key] = self.permissions.check(f"user:{user_id}", "editor", f"document:{doc_id}")
        if not allowed:
            raise AccessDenied(f"document {doc_id}")

    def visible(self, user_id: str, candidates: list) -> list:
        """List pages: one batched call, not one check per row."""
        allowed = set(self.permissions.batch_check(f"user:{user_id}", "viewer",
                                                   [f"document:{d.id}" for d in candidates]))
        return [d for d in candidates if f"document:{d.id}" in allowed]

    def share(self, doc_id: str, with_user_id: str) -> None:
        self.permissions.write(f"document:{doc_id}", "editor", f"user:{with_user_id}")   # a new tuple

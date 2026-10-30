import hashlib

VIRTUAL_SHARDS = 1024

def bucket_of(tenant_id: int) -> int:
    # Stable everywhere — Python's built-in hash() is randomized per process for strings.
    digest = hashlib.sha256(str(tenant_id).encode()).digest()
    return int.from_bytes(digest[:8], "big") % VIRTUAL_SHARDS

class ShardRouter:
    def __init__(self, bucket_to_db: dict[int, str], pinned: dict[int, str] | None = None):
        self.bucket_to_db = bucket_to_db
        self.pinned = pinned or {}

    def database_for(self, tenant_id: int) -> str:
        return self.pinned.get(tenant_id) or self.bucket_to_db[bucket_of(tenant_id)]

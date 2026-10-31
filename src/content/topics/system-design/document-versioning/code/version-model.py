# Content-addressed versions: full snapshots every N saves, deltas in between; head moves
# with a CAS.
import hashlib

SNAPSHOT_EVERY = 10


class ConcurrentEdit(Exception):
    pass


def save(conn, page_id: int, expected_head: int, content: str, author_id: int, message: str) -> int:
    data = content.encode()
    h = hashlib.sha256(data).hexdigest()
    with conn.transaction():
        # identical content (or a restore) stores nothing
        if not blobs.exists(h):
            parent = blobs.get(versions.get(expected_head).content_hash)
            if parent.chain_length + 1 >= SNAPSHOT_EVERY:
                blobs.put_full(h, data)
            else:
                blobs.put_delta(
                    h, parent.hash, delta(read(parent.hash).encode(), data), parent.chain_length + 1
                )
        vid = versions.insert(conn, page_id, expected_head, h, author_id, message)
        if (
            conn.execute(
                "UPDATE pages SET head_id = %s WHERE id = %s AND head_id = %s",
                (vid, page_id, expected_head),
            ).rowcount
            == 0
        ):
            # rolls back; the client must rebase or merge
            raise ConcurrentEdit(page_id)
        return vid


def read(h: str) -> str:
    """Walk back to the nearest full snapshot, then apply deltas forwards."""
    chain, b = [], blobs.get(h)
    while b.is_delta:
        chain.append(b)
        b = blobs.get(b.base_hash)
    content = b.data
    for d in reversed(chain):
        content = apply_delta(content, d.data)
    return content.decode()

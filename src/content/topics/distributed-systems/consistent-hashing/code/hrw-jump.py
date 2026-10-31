import hashlib

def hash64(s: str) -> int:
    """First 8 bytes of MD5, big-endian, unsigned."""
    return int.from_bytes(hashlib.md5(s.encode()).digest()[:8], "big")

def rendezvous(key: str, nodes: list[str]) -> str:
    """Highest-random-weight hashing: the node with the highest score for this key wins."""
    return max(nodes, key=lambda node: hash64(f"{node}|{key}"))

def jump_hash(key: int, buckets: int) -> int:
    """Jump consistent hash (Lamping & Veach): a bucket in [0, buckets), no ring, no memory."""
    b, j = -1, 0
    while j < buckets:
        b = j
        key = (key * 2862933555777941757 + 1) & 0xFFFFFFFFFFFFFFFF
        j = int((b + 1) * (float(1 << 31) / float((key >> 33) + 1)))
    return b

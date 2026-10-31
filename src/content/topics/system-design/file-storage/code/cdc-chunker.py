import hashlib

def _gear_table() -> list[int]:
    """256 pseudo-random 64-bit values (xorshift64*), fixed so every client cuts identically."""
    x, table = 0x9E3779B97F4A7C15, []
    for _ in range(256):
        x ^= (x >> 12); x ^= (x << 25) & 0xFFFFFFFFFFFFFFFF; x ^= (x >> 27)
        table.append((x * 0x2545F4914F6CDD1D) & 0xFFFFFFFFFFFFFFFF)
    return table

GEAR = _gear_table()

def chunk(data: bytes, min_size: int = 2048, avg_bits: int = 13, max_size: int = 65536) -> list[bytes]:
    """Content-defined chunking: cut where a rolling hash of the last bytes matches a pattern.
    Boundaries depend on content, not offsets, so an insertion only changes nearby chunks.
    avg_bits=13 → average chunk ≈ 8 KiB (+ min_size)."""
    mask = ((1 << avg_bits) - 1) << (64 - avg_bits)          # test the high bits
    chunks, start, h = [], 0, 0
    for i, b in enumerate(data):
        h = ((h << 1) + GEAR[b]) & 0xFFFFFFFFFFFFFFFF         # gear hash: old bytes shift out
        size = i - start + 1
        if (size >= min_size and (h & mask) == 0) or size >= max_size:
            chunks.append(data[start:i + 1])
            start, h = i + 1, 0
    if start < len(data):
        chunks.append(data[start:])
    return chunks

def chunk_ids(data: bytes) -> list[str]:
    """Each chunk is stored once, named by its SHA-256 — identical chunks dedupe across files and users."""
    return [hashlib.sha256(c).hexdigest() for c in chunk(data)]

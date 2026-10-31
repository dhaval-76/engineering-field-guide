import threading

ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
SPACE = 62 ** 7                     # 3,521,614,606,208 seven-character codes
# coprime with 62^7 → multiplication mod 62^7 is a bijection
MULTIPLIER = 1_580_030_173

def encode(n: int) -> str:
    """Fixed-width base62 so every code has 7 characters."""
    out = []
    for _ in range(7):
        n, r = divmod(n, 62)
        out.append(ALPHABET[r])
    return "".join(reversed(out))

def decode(code: str) -> int:
    n = 0
    for c in code:
        i = ALPHABET.find(c)
        if i < 0:
            raise ValueError(f"not base62: {c}")
        n = n * 62 + i
    return n

def scrambled(id_: int) -> str:
    """Spread consecutive ids across the code space so codes aren't sequential (not encryption)."""
    if not 0 <= id_ < SPACE:
        raise ValueError("id out of range")
    return encode(id_ * MULTIPLIER % SPACE)          # Python ints don't overflow

class IdRange:
    """Each instance reserves 10,000 ids at a time, so creating a link needs no shared call per link."""

    def __init__(self, conn):
        self._conn, self._next, self._end = conn, 0, 0
        self._lock = threading.Lock()

    def next_id(self) -> int:
        with self._lock:
            if self._next == self._end:
                (start,) = self._conn.execute(
                    "UPDATE id_blocks SET next_value = next_value + 10000 WHERE name = 'links' "
                    "RETURNING next_value - 10000").fetchone()
                self._next, self._end = start, start + 10_000
            self._next += 1
            return self._next - 1

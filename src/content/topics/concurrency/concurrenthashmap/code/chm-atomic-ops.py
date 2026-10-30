import threading
from collections import Counter

# dict operations that are single bytecodes (d[k] = v, d.get(k)) are effectively atomic under the GIL,
# but read-modify-write is not — guard it:
_lock = threading.Lock()
counts: Counter = Counter()

def record(word: str) -> None:
    with _lock:
        counts[word] += 1

# dict.setdefault(k, v) is atomic for built-in dicts (when the key's hash/eq are pure-C types)

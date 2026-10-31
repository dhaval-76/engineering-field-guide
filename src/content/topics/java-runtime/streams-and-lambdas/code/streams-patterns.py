from collections import Counter, defaultdict
from itertools import groupby, islice

totals: dict[str, int] = defaultdict(int)
for o in orders:
    if o.status != "CANCELLED":
        totals[o.customer_id] += o.total_paise
top3 = [cid for cid, _ in sorted(totals.items(), key=lambda kv: kv[1], reverse=True)[:3]]

word_counts = Counter(words)

# Generators are lazy, like Java streams: nothing runs until consumed, and you can stop
# early.
first_long_a = next((s.upper() for s in strings if s.startswith("A") and len(s) > 5), "NONE")

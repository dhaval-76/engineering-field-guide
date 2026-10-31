import random

# In Python, a strategy is usually just a callable.
def fixed(attempt: int) -> float: return 0.2
def exponential(attempt: int) -> float: return 0.1 * 2 ** min(attempt, 10)
def jittered(attempt: int) -> float: return random.uniform(0, 0.1 * 2 ** min(attempt, 10))

# retry(fetch_orders, backoff=jittered)
# Built-in strategies are everywhere: sorted(key=...), max(key=...), functools.cmp_to_key, ...
orders.sort(key=lambda o: o.created_at, reverse=True)

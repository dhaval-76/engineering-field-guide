# Reproduce exhaustion: pool of 2, ten requests that each hold a connection for 2 s, wait
# limit 3 s.
import time
from concurrent.futures import ThreadPoolExecutor
from sqlalchemy import create_engine, text
from sqlalchemy.exc import TimeoutError as PoolTimeout

engine = create_engine("postgresql+psycopg://shop@localhost/shop",
                       pool_size=2, max_overflow=0, pool_timeout=3)

def request(_):
    start = time.monotonic()
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT pg_sleep(2)"))
        return f"ok after {int((time.monotonic() - start) * 1000)} ms"
    except PoolTimeout:
        return "timed out waiting for a connection"

with ThreadPoolExecutor(max_workers=10) as pool:
    print("\n".join(pool.map(request, range(10))))
# Expect: 2 finish after ~2 s, 2 after ~4 s (waited ~2 s), the rest time out after 3 s of
# waiting.

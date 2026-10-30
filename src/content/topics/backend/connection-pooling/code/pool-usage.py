from sqlalchemy import create_engine, text

engine = create_engine(
    "postgresql+psycopg://shop@db.internal/shop",
    pool_size=20, max_overflow=0,      # fixed size
    pool_timeout=3,                    # seconds to wait for a connection
    pool_recycle=1680, pool_pre_ping=True,
)

with engine.begin() as conn:           # borrow, BEGIN … COMMIT/ROLLBACK, return to pool
    conn.execute(text("UPDATE accounts SET balance = balance - :a WHERE id = :id"), {"a": amt, "id": src})

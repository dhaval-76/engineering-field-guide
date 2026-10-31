import json, random
import redis

r = redis.Redis()
TTL = 600

def get_product(conn, product_id: int) -> dict | None:
    key = f"product:v1:{product_id}"
    try:
        cached = r.get(key)
        if cached is not None:
            return json.loads(cached)              # may be None: a cached "not found"
    except redis.ConnectionError:
        pass                                       # degrade to the database

    row = conn.execute("SELECT id, name, price::text FROM products WHERE id = %s", (product_id,)).fetchone()
    value = None if row is None else {"id": row[0], "name": row[1], "price": row[2]}
    ttl = 30 if value is None else int(TTL * random.uniform(0.9, 1.1))
    try:
        r.set(key, json.dumps(value), ex=ttl)
    except redis.ConnectionError:
        pass
    return value

def change_price(pool, product_id: int, price: str) -> None:
    with pool.connection() as conn, conn.transaction():
        conn.execute("UPDATE products SET price = %s WHERE id = %s", (price, product_id))
    r.delete(f"product:v1:{product_id}")           # after the transaction has committed

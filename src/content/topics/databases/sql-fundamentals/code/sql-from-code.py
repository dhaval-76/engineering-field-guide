# psycopg 3: %s placeholders (not Python string formatting!)
import psycopg

def place_order(pool, customer_id: int, product_id: int, qty: int) -> int:
    with pool.connection() as conn:          # commits on success, rolls back on exception
        with conn.transaction():
            cur = conn.execute(
                "UPDATE products SET stock = stock - %s WHERE id = %s AND stock >= %s",
                (qty, product_id, qty),
            )
            if cur.rowcount == 0:
                raise OutOfStock(product_id)

            row = conn.execute(
                "INSERT INTO orders (customer_id, total) "
                "SELECT %s, price * %s FROM products WHERE id = %s RETURNING id",
                (customer_id, qty, product_id),
            ).fetchone()
            return row[0]

# ✗ conn.execute(f"SELECT * FROM customers WHERE email = '{email}'")  — SQL injection

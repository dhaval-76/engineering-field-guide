# psycopg 3
def debit(conn, account_id: str, amount: Decimal) -> bool:
    with conn.transaction():
        cur = conn.execute(
            "UPDATE accounts SET balance = balance - %s WHERE id = %s AND balance >= %s",
            (amount, account_id, amount),
        )
        return cur.rowcount == 1

# Pessimistic: lock both rows (SELECT … FOR UPDATE) in a consistent order for the whole
# transaction.
class InsufficientFunds(Exception):
    pass

def transfer(conn, from_id: str, to_id: str, amount: int) -> None:
    # commit releases both row locks
    with conn.transaction():
        rows = dict(conn.execute("SELECT id, balance FROM accounts WHERE id = ANY(%s) ORDER BY id FOR UPDATE",
                                 # consistent order → no deadlock
                                 ([from_id, to_id],)).fetchall())
        if rows[from_id] < amount:
            raise InsufficientFunds(from_id)
        conn.execute("UPDATE accounts SET balance = balance - %s WHERE id = %s", (amount, from_id))
        conn.execute("UPDATE accounts SET balance = balance + %s WHERE id = %s", (amount, to_id))
        conn.execute("INSERT INTO transfers (from_id, to_id, amount) VALUES (%s, %s, %s)", (from_id, to_id, amount))

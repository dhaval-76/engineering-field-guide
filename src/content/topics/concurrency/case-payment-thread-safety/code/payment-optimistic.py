# Optimistic: read with a version, write only if the version is unchanged; retry on
# conflict.
import random
import time

class InsufficientFunds(Exception): pass
class TooMuchContention(Exception): pass

def debit(conn, account_id: str, amount: int, attempts: int = 3) -> None:
    for i in range(1, attempts + 1):
        balance, version = conn.execute("SELECT balance, version FROM accounts WHERE id = %s", (account_id,)).fetchone()
        if balance < amount:
            raise InsufficientFunds(account_id)
        updated = conn.execute(
            "UPDATE accounts SET balance = balance - %s, version = version + 1 WHERE id = %s AND version = %s",
            (amount, account_id, version)).rowcount
        if updated == 1:
            # nobody changed the row since we read it
            return
        # lost the race: back off with jitter, re-read
        time.sleep(random.uniform(0, 0.02 * i))
    raise TooMuchContention(account_id)

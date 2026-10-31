import threading

# Check-then-act on a balance: two threads both see 1000 ≥ 500 and both withdraw → 500,
# not 0.

class InsufficientFunds(Exception):
    pass

class Account:
    """✓ A lock makes the check and the update one step."""

    def __init__(self, balance: int):
        self.balance = balance
        self._lock = threading.Lock()

    def withdraw(self, amount: int) -> None:
        with self._lock:
            if self.balance < amount:
                raise InsufficientFunds()
            self.balance -= amount

# ✓ Several app servers: the database must do it atomically UPDATE accounts SET balance =
# balance - %s WHERE id = %s AND balance >= %s;   -- 0 rows ⇒ insufficient

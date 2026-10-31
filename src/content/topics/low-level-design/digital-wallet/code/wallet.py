import threading
import uuid
from dataclasses import dataclass

class InsufficientFunds(Exception):
    pass

@dataclass
class _Account:
    id: str
    balance: int = 0        # paise; cached sum of ledger entries
    held: int = 0           # reserved by authorisations
    lock: threading.Lock = None

    def __post_init__(self):
        self.lock = threading.Lock()

    @property
    def available(self) -> int:
        return self.balance - self.held

class WalletService:
    """Double-entry wallet: every movement is a balanced pair of ledger entries."""

    def __init__(self):
        self._accounts: dict[str, _Account] = {}
        # (txn_id, account, +credit/−debit)
        self.ledger: list[tuple[str, str, int]] = []
        self._idem: dict[str, str] = {}
        # hold_id -> (account, amount)
        self._holds: dict[str, tuple[str, int]] = {}
        self._registry = threading.Lock()

    def open(self, account_id: str) -> None:
        with self._registry:
            self._accounts.setdefault(account_id, _Account(account_id))

    def balance(self, account_id: str) -> int:
        return self._accounts[account_id].balance

    def available(self, account_id: str) -> int:
        return self._accounts[account_id].available

    def deposit(self, account_id: str, amount: int, key: str) -> str:
        self.open("system:bank-in")
        return self._transfer("system:bank-in", account_id, amount, key, allow_negative=True)

    def transfer(self, src: str, dst: str, amount: int, key: str) -> str:
        return self._transfer(src, dst, amount, key)

    def _transfer(self, src, dst, amount, key, allow_negative=False, release_held=0) -> str:
        if amount <= 0 or src == dst:
            raise ValueError("invalid transfer")
        txn_id = str(uuid.uuid4())
        # claim the idempotency key atomically
        with self._registry:
            if key in self._idem:
                return self._idem[key]
            self._idem[key] = txn_id
        a, b = self._accounts[src], self._accounts[dst]
        # global lock order: no deadlock
        first, second = sorted((a, b), key=lambda acc: acc.id)
        try:
            with first.lock, second.lock:
                if not allow_negative and a.available + release_held < amount:
                    raise InsufficientFunds(src)
                a.held -= release_held
                a.balance -= amount
                b.balance += amount
                self.ledger += [(txn_id, src, -amount), (txn_id, dst, amount)]
        except Exception:
            with self._registry:
                # failed: allow a corrected retry
                del self._idem[key]
            raise
        return txn_id

    def hold(self, account_id: str, amount: int) -> str:
        acc = self._accounts[account_id]
        with acc.lock:
            if acc.available < amount:
                raise InsufficientFunds(account_id)
            acc.held += amount
        hold_id = str(uuid.uuid4())
        with self._registry:
            self._holds[hold_id] = (account_id, amount)
        return hold_id

    def capture(self, hold_id: str, merchant: str, amount: int, key: str) -> str:
        with self._registry:
            # a second capture finds nothing
            claimed = self._holds.pop(hold_id, None)
        if claimed is None:
            raise KeyError("unknown or finished hold")
        account_id, held = claimed
        try:
            if amount > held:
                raise ValueError("capture exceeds hold")
            return self._transfer(account_id, merchant, amount, key, release_held=held)
        except Exception:
            with self._registry:
                self._holds[hold_id] = claimed
            raise

    def release(self, hold_id: str) -> None:
        with self._registry:
            claimed = self._holds.pop(hold_id, None)
        if claimed:
            acc = self._accounts[claimed[0]]
            with acc.lock:
                acc.held -= claimed[1]

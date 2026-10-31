import threading
import uuid
from collections import defaultdict
from dataclasses import dataclass

@dataclass(frozen=True)
class Expense:
    id: str
    paid_by: str
    amount: int
    owed: dict
    description: str

class Group:
    """Net balance per member: + is owed money, − owes money. Always sums to zero."""

    def __init__(self, id: str):
        self.id = id
        self.expenses: list[Expense] = []
        self._net: defaultdict[str, int] = defaultdict(int)
        self._lock = threading.Lock()

    def add_expense(self, paid_by: str, amount: int, participants: list[str], split, description: str) -> Expense:
        owed = split(amount, participants)
        e = Expense(str(uuid.uuid4()), paid_by, amount, owed, description)
        with self._lock:
            self.expenses.append(e)
            self._net[paid_by] += amount                     # payer fronted the whole amount
            for person, share in owed.items():
                self._net[person] -= share
        return e

    def record_payment(self, frm: str, to: str, amount: int) -> None:   # a settle-up is just a transfer
        with self._lock:
            self._net[frm] += amount
            self._net[to] -= amount

    def net_balances(self) -> dict[str, int]:
        with self._lock:
            return dict(self._net)

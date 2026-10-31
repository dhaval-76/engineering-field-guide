from dataclasses import dataclass, replace
from functools import reduce

@dataclass(frozen=True)
class Opened:
    account_id: str
    owner: str

@dataclass(frozen=True)
class Deposited:
    cents: int

@dataclass(frozen=True)
class Withdrawn:
    cents: int

Event = Opened | Deposited | Withdrawn

@dataclass(frozen=True)
class Account:
    id: str | None = None
    open: bool = False
    balance: int = 0
    version: int = 0

    # pure: events are facts, no validation
    def apply(self, e: Event) -> "Account":
        match e:
            case Opened(account_id=aid):
                return Account(aid, True, 0, self.version + 1)
            case Deposited(cents=c):
                return replace(self, balance=self.balance + c, version=self.version + 1)
            case Withdrawn(cents=c):
                return replace(self, balance=self.balance - c, version=self.version + 1)
        raise ValueError(f"unknown event {e}")

    # Command handlers: validate against current state, return new events.
    def open_account(self, account_id: str, owner: str) -> list[Event]:
        if self.open:
            raise ValueError("already open")
        return [Opened(account_id, owner)]

    def deposit(self, cents: int) -> list[Event]:
        if not self.open:
            raise ValueError("account not open")
        if cents <= 0:
            raise ValueError("amount must be positive")
        return [Deposited(cents)]

    def withdraw(self, cents: int) -> list[Event]:
        if not self.open:
            raise ValueError("account not open")
        if cents > self.balance:
            raise ValueError("insufficient funds")
        return [Withdrawn(cents)]

def replay(history: list[Event]) -> Account:
    return reduce(Account.apply, history, Account())

def handle_withdraw(store, account_id: str, cents: int) -> None:
    state = replay(store.load(account_id))
    # optimistic concurrency
    store.append(account_id, state.version, state.withdraw(cents))

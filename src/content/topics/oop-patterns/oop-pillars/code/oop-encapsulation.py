class BankAccount:
    """Python has no enforced private fields; a leading underscore is the convention
    that says "internal — use the methods". Properties give read-only access."""

    def __init__(self, account_number: str, opening_paise: int):
        if opening_paise < 0:
            raise ValueError("opening balance < 0")
        self.account_number = account_number
        self._balance_paise = opening_paise

    def deposit(self, paise: int) -> None:
        if paise <= 0:
            raise ValueError("deposit must be positive")
        self._balance_paise += paise

    def withdraw(self, paise: int) -> None:
        if paise <= 0:
            raise ValueError("withdrawal must be positive")
        if paise > self._balance_paise:
            raise ValueError("insufficient funds")
        self._balance_paise -= paise

    @property
    def balance(self) -> int:
        return self._balance_paise

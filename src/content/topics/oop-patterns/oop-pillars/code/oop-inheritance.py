class SavingsAccount(BankAccount):
    def __init__(self, number: str, opening_paise: int, annual_rate: float):
        super().__init__(number, opening_paise)
        self._annual_rate = annual_rate
        self._withdrawals_left = 3

    def apply_monthly_interest(self) -> None:
        self.deposit(round(self.balance * self._annual_rate / 12))

    def withdraw(self, paise: int) -> None:
        if self._withdrawals_left == 0:
            raise ValueError("monthly limit reached")
        super().withdraw(paise)
        self._withdrawals_left -= 1

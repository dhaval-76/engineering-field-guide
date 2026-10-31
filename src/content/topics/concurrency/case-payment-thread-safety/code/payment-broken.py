# ✗ BROKEN — read-modify-write on shared state, and money as a float
def debit(account_id: str, amount: float) -> None:
    account = db.find_account(account_id)      # request A and request B both read 1000.0
    if account.balance >= amount:
        account.balance -= amount              # both compute 100.0
        db.save_account(account)               # last write wins: one debit is lost
# 0.1 + 0.2 == 0.30000000000000004 — store integer minor units (paise/cents) or decimal.Decimal.
# A threading.Lock wouldn't help: several worker processes or servers each load their own copy.

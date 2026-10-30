/** Checked: an expected business outcome the caller should handle (show a message, offer a smaller amount). */
public class InsufficientFundsException extends Exception {
    private final long shortfallPaise;
    public InsufficientFundsException(long shortfallPaise) {
        super("insufficient funds, short by " + shortfallPaise + " paise");
        this.shortfallPaise = shortfallPaise;
    }
    public long shortfallPaise() { return shortfallPaise; }
}

/** Unchecked: a broken invariant / programming error — callers can't sensibly recover. */
public class AccountFrozenException extends IllegalStateException {
    public AccountFrozenException(String accountId) { super("account " + accountId + " is frozen"); }
}

public void withdraw(long paise) throws InsufficientFundsException {
    if (frozen) throw new AccountFrozenException(id);
    if (paise > balance) throw new InsufficientFundsException(paise - balance);
    balance -= paise;
}

// Many teams instead model expected outcomes as return values (sealed result types),
// keeping exceptions for the unexpected:
sealed interface WithdrawResult permits Ok, Insufficient {}
record Ok(long newBalance) implements WithdrawResult {}
record Insufficient(long shortfall) implements WithdrawResult {}

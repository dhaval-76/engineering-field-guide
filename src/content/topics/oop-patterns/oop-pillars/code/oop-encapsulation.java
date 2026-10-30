public class BankAccount {
    private final String accountNumber;
    private long balancePaise;               // private: only this class can change it

    public BankAccount(String accountNumber, long openingPaise) {
        if (openingPaise < 0) throw new IllegalArgumentException("opening balance < 0");
        this.accountNumber = accountNumber;
        this.balancePaise = openingPaise;
    }

    public void deposit(long paise) {
        if (paise <= 0) throw new IllegalArgumentException("deposit must be positive");
        balancePaise += paise;
    }

    public void withdraw(long paise) {
        if (paise <= 0) throw new IllegalArgumentException("withdrawal must be positive");
        if (paise > balancePaise) throw new IllegalStateException("insufficient funds");
        balancePaise -= paise;
    }

    public long balance() { return balancePaise; }   // read access, no setter
}

// account.balancePaise = -1;   ← does not compile: the invariant "balance >= 0" is protected

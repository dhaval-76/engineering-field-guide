// Check-then-act on a balance: two threads both see 1000 ≥ 500 and both withdraw → balance 500, not 0.

// ✓ Lock: the check and the update happen as one step
class Account {
    private long balance;
    synchronized void withdraw(long amount) {
        if (balance < amount) throw new InsufficientFundsException();
        balance -= amount;
    }
}

// ✓ CAS loop: read, compute, publish only if nobody changed it meanwhile
class LockFreeAccount {
    private final AtomicLong balance = new AtomicLong();
    void withdraw(long amount) {
        while (true) {
            long current = balance.get();
            if (current < amount) throw new InsufficientFundsException();
            if (balance.compareAndSet(current, current - amount)) return;   // lost the race → retry
        }
    }
}

// ✓ Several app servers: the database must do it atomically
// UPDATE accounts SET balance = balance - :amt WHERE id = :id AND balance >= :amt;   -- 0 rows ⇒ insufficient

sealed interface AccountEvent permits Opened, Deposited, Withdrawn {}
record Opened(String accountId, String owner) implements AccountEvent {}
record Deposited(long cents) implements AccountEvent {}
record Withdrawn(long cents) implements AccountEvent {}

/** State is never stored directly: it is the fold of the account's events. */
record Account(String id, boolean open, long balance, int version) {
    static final Account EMPTY = new Account(null, false, 0, 0);

    // pure: no validation, events are facts
    Account apply(AccountEvent e) {
        return switch (e) {
            case Opened o    -> new Account(o.accountId(), true, 0, version + 1);
            case Deposited d -> new Account(id, open, balance + d.cents(), version + 1);
            case Withdrawn w -> new Account(id, open, balance - w.cents(), version + 1);
        };
    }

    static Account replay(List<AccountEvent> history) {
        Account a = EMPTY;
        for (AccountEvent e : history) a = a.apply(e);
        return a;
    }

    // Command handlers: check invariants against current state, return new events (or
    // reject).
    List<AccountEvent> open(String accountId, String owner) {
        if (open) throw new IllegalStateException("already open");
        return List.of(new Opened(accountId, owner));
    }

    List<AccountEvent> deposit(long cents) {
        if (!open) throw new IllegalStateException("account not open");
        if (cents <= 0) throw new IllegalArgumentException("amount must be positive");
        return List.of(new Deposited(cents));
    }

    List<AccountEvent> withdraw(long cents) {
        if (!open) throw new IllegalStateException("account not open");
        if (cents > balance) throw new IllegalStateException("insufficient funds");
        return List.of(new Withdrawn(cents));
    }
}

// Handling a command: load → decide → append with the version we read (optimistic
// concurrency).
void withdraw(EventStore store, String accountId, long cents) {
    List<AccountEvent> history = store.load(accountId);
    Account account = Account.replay(history);
    List<AccountEvent> newEvents = account.withdraw(cents);
    // fails if someone appended meanwhile → retry
    store.append(accountId, account.version(), newEvents);
}

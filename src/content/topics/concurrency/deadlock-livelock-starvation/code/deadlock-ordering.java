// ✗ DEADLOCK — transfer(a, b) and transfer(b, a) lock in opposite orders
void transfer(Account from, Account to, BigDecimal amount) {
    synchronized (from) {
        synchronized (to) {            // T1 holds A, wants B; T2 holds B, wants A → both wait forever
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// ✓ FIX — one global order (by id), whatever the direction of the transfer
void transferSafely(Account from, Account to, BigDecimal amount) {
    Account first  = from.id().compareTo(to.id()) < 0 ? from : to;
    Account second = first == from ? to : from;
    synchronized (first) {
        synchronized (second) {        // every thread locks the lower id first → no cycle possible
            from.debit(amount);
            to.credit(amount);
        }
    }
}

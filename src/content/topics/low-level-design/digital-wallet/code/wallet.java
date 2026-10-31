// amount: + credit, − debit (paise)
record Entry(String txnId, String account, long amount, String type) {}

class InsufficientFunds extends RuntimeException {
    InsufficientFunds(String a) {
        super("insufficient funds in " + a);
    }
}

/** Double-entry wallet: every money movement is a balanced set of entries; balances are derived. */
class WalletService {
    private static final class Account {
        final String id;
        long balance; // cached sum of entries
        long held; // reserved by authorisations, not yet captured

        Account(String id) {
            this.id = id;
        }

        long available() {
            return balance - held;
        }
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    // append-only
    private final List<Entry> ledger = Collections.synchronizedList(new ArrayList<>());
    // key → txnId
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();
    // holdId → {amount}
    private final Map<String, long[]> holds = new ConcurrentHashMap<>();
    private final Map<String, String> holdAccount = new ConcurrentHashMap<>();

    void open(String id) {
        accounts.putIfAbsent(id, new Account(id));
    }

    long balance(String id) {
        Account a = accounts.get(id);
        synchronized (a) {
            return a.balance;
        }
    }

    long available(String id) {
        Account a = accounts.get(id);
        synchronized (a) {
            return a.available();
        }
    }

    /**
     * Top-up from an external payment: money enters through a system account so the books balance.
     */
    String deposit(String id, long amount, String idempotencyKey) {
        return transfer("system:bank-in", id, amount, idempotencyKey, true, 0);
    }

    String transfer(String from, String to, long amount, String idempotencyKey) {
        return transfer(from, to, amount, idempotencyKey, false, 0);
    }

    /** releaseHeld: a hold on `from` released in the same critical section (used by capture). */
    private String transfer(
            String from,
            String to,
            long amount,
            String key,
            boolean allowNegativeSource,
            long releaseHeld) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (from.equals(to)) throw new IllegalArgumentException("same account");
        String txnId = UUID.randomUUID().toString();
        String existing = idempotency.putIfAbsent(key, txnId);
        // retry of a request we already handled
        if (existing != null) return existing;

        accounts.computeIfAbsent(from, Account::new);
        Account a = accounts.get(from), b = accounts.get(to);
        // lock in a global order: no deadlock
        Account first = a.id.compareTo(b.id) < 0 ? a : b;
        Account second = first == a ? b : a;
        try {
            synchronized (first) {
                synchronized (second) {
                    if (!allowNegativeSource && a.available() + releaseHeld < amount)
                        throw new InsufficientFunds(from);
                    a.held -= releaseHeld;
                    a.balance -= amount;
                    b.balance += amount;
                    ledger.add(new Entry(txnId, from, -amount, "TRANSFER"));
                    ledger.add(new Entry(txnId, to, amount, "TRANSFER"));
                }
            }
        } catch (RuntimeException e) {
            // failed: let a corrected retry run
            idempotency.remove(key, txnId);
            throw e;
        }
        return txnId;
    }

    /** Authorise: reserve funds (e.g. a ride's estimated fare) without moving them yet. */
    String hold(String account, long amount) {
        Account a = accounts.get(account);
        synchronized (a) {
            if (a.available() < amount) throw new InsufficientFunds(account);
            a.held += amount;
        }
        String holdId = UUID.randomUUID().toString();
        holds.put(holdId, new long[] {amount});
        holdAccount.put(holdId, account);
        return holdId;
    }

    /**
     * Capture up to the held amount to a merchant and release the rest — atomically, under the same
     * locks.
     */
    String capture(String holdId, String merchant, long amount, String idempotencyKey) {
        // claim the hold: a second capture finds nothing
        long[] h = holds.remove(holdId);
        if (h == null) throw new IllegalStateException("unknown or finished hold");
        String account = holdAccount.remove(holdId);
        try {
            if (amount > h[0]) throw new IllegalArgumentException("capture exceeds hold");
            return transfer(account, merchant, amount, idempotencyKey, false, h[0]);
        } catch (RuntimeException e) {
            // nothing moved: restore the hold
            holdAccount.put(holdId, account);
            holds.put(holdId, h);
            throw e;
        }
    }

    void release(String holdId) {
        long[] h = holds.remove(holdId);
        // already captured/released: idempotent
        if (h == null) return;
        Account a = accounts.get(holdAccount.remove(holdId));
        synchronized (a) {
            a.held -= h[0];
        }
    }

    /**
     * Invariant check: every transaction's entries sum to zero, and balances equal the sum of
     * entries.
     */
    boolean booksBalance() {
        synchronized (ledger) {
            Map<String, Long> byTxn = new HashMap<>(), byAccount = new HashMap<>();
            for (Entry e : ledger) {
                byTxn.merge(e.txnId(), e.amount(), Long::sum);
                byAccount.merge(e.account(), e.amount(), Long::sum);
            }
            return byTxn.values().stream().allMatch(v -> v == 0)
                    && accounts.values().stream()
                            .allMatch(acc -> byAccount.getOrDefault(acc.id, 0L) == balance(acc.id));
        }
    }
}

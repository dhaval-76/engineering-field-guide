@Service
class Ledger {
    private final JdbcTemplate jdbc;

    /** Moves `amount` minor units from one account to another exactly once per idempotency key. */
    @Transactional
    public UUID transfer(String idempotencyKey, long from, long to, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        UUID journalId = UUID.randomUUID();

        // 1. Claim the key. A retry (or a concurrent duplicate) inserts nothing → return the original.
        int claimed = jdbc.update(
            "INSERT INTO journal (id, idempotency_key, kind) VALUES (?, ?, 'transfer') ON CONFLICT (idempotency_key) DO NOTHING",
            journalId, idempotencyKey);
        if (claimed == 0) {
            return jdbc.queryForObject("SELECT id FROM journal WHERE idempotency_key = ?", UUID.class, idempotencyKey);
        }

        // 2. Lock both accounts in a consistent order (lower id first) — no deadlocks between opposite transfers.
        long first = Math.min(from, to), second = Math.max(from, to);
        jdbc.queryForList("SELECT id FROM accounts WHERE id IN (?, ?) ORDER BY id FOR UPDATE", first, second);

        // 3. Debit with a guard; 0 rows → insufficient funds → the whole transaction (including the claim) rolls back.
        int debited = jdbc.update(
            "UPDATE accounts SET balance = balance - ? WHERE id = ? AND (allow_negative OR balance >= ?)",
            amount, from, amount);
        if (debited == 0) throw new InsufficientFundsException(from, amount);
        jdbc.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, to);

        // 4. The immutable record: entries sum to zero.
        jdbc.update("INSERT INTO entries (journal_id, account_id, amount) VALUES (?, ?, ?), (?, ?, ?)",
                    journalId, from, -amount, journalId, to, amount);

        // 5. Event for other services, in the same transaction (outbox).
        jdbc.update("INSERT INTO outbox (id, type, payload) VALUES (gen_random_uuid(), 'TransferCompleted', ?::jsonb)",
                    "{\"journalId\":\"%s\",\"amount\":%d}".formatted(journalId, amount));
        return journalId;
    }
}
// Note: a failed transfer rolls back the key claim too, so the client may retry it after topping up.
// To make declines replayable instead, store the decline outcome under the key in a separate transaction.

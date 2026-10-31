@Service
class IdempotentExecutor {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    /** Runs `action` at most once per (client, key); repeats get the stored response. */
    public StoredResponse execute(String clientId, String key, String requestHash,
                                  Supplier<StoredResponse> action) {
        // 1. Claim the key atomically. The primary key makes concurrent duplicates lose
        // this race.
        int claimed = jdbc.update("""
            INSERT INTO idempotency_keys(client_id, key, request_hash, status, locked_until)
            VALUES (?, ?, ?, 'IN_PROGRESS', now() + interval '30 seconds')
            ON CONFLICT (client_id, key) DO NOTHING""", clientId, key, requestHash);

        if (claimed == 0) {
            Existing e = load(clientId, key);
            // → 422
            if (!e.requestHash().equals(requestHash)) throw new IdempotencyKeyReused();
            // replay
            if (e.status().equals("DONE")) return e.response();
            // → 409, retry later
            if (e.lockedUntil().isAfter(Instant.now())) throw new RequestInProgress();
            // stale: take it over
            if (!reclaim(clientId, key)) throw new RequestInProgress();
        }

        // 2. Do the work and record the outcome in ONE transaction, so they can't
        // disagree.
        try {
            return tx.execute(status -> {
                StoredResponse result = action.get();
                jdbc.update("""
                    UPDATE idempotency_keys SET status = 'DONE', response_code = ?, response_body = ?::jsonb
                    WHERE client_id = ? AND key = ?""", result.code(), result.json(), clientId, key);
                return result;
            });
        } catch (BusinessRejection e) {
            // A decline (402, insufficient funds) is a final answer: store it so a retry
            // gets the same answer.
            StoredResponse rejected = e.toResponse();
            jdbc.update("""
                UPDATE idempotency_keys SET status = 'DONE', response_code = ?, response_body = ?::jsonb
                WHERE client_id = ? AND key = ?""", rejected.code(), rejected.json(), clientId, key);
            return rejected;
        } catch (RuntimeException e) {
            // Unexpected failure (bug, dependency down): nothing was committed, so
            // release the key and let the client's retry run the action again.
            jdbc.update("""
                    DELETE FROM idempotency_keys
                    WHERE client_id = ? AND key = ? AND status = 'IN_PROGRESS'""",
                        clientId, key);
            throw e;
        }
    }
}

@PostMapping("/payments")
public ResponseEntity<PaymentResult> pay(@RequestHeader("Idempotency-Key") String key,
                                         @RequestBody PaymentRequest req) {
    return ResponseEntity.ok(payments.payOnce(key, req));
}

@Transactional
public PaymentResult payOnce(String key, PaymentRequest req) {
    // INSERT … ON CONFLICT DO NOTHING: exactly one request with this key gets to proceed
    boolean first = jdbc.update(
        "INSERT INTO idempotency_keys(key, request_hash, status) VALUES (?, ?, 'IN_PROGRESS') ON CONFLICT (key) DO NOTHING",
        key, hash(req)) == 1;

    // same key: replay the stored result
    if (!first) return previousResultOrConflict(key, req);
                                                             // (or 409 if still in
                                                             // progress / different body)
    // atomic UPDATE + ledger row, same transaction
    PaymentResult result = debitAndRecord(req);
    jdbc.update("UPDATE idempotency_keys SET status = 'DONE', response = ? WHERE key = ?", toJson(result), key);
    return result;
}

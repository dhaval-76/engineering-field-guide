@Service
class Payments {
    private final JdbcTemplate jdbc;
    private final PaymentGateway gateway;

    @Transactional
    public void chargeOnce(PaymentRequested e) {
        // 1. Claim the event id in THIS transaction. A duplicate delivery inserts
        // nothing.
        int claimed = jdbc.update(
            "INSERT INTO processed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING", e.eventId());
        // already handled: skip, offset gets committed
        if (claimed == 0) return;

        // 2. The external side effect carries its own idempotency key, so even a retry
        //    after a crash between the gateway call and our commit can't double-charge.
        Charge c = gateway.charge(e.customerId(), e.amount(), "payment-" + e.eventId());

        // 3. Record the outcome in the same transaction as the claim.
        jdbc.update("INSERT INTO payments (event_id, charge_id, amount) VALUES (?, ?, ?)",
                    e.eventId(), c.id(), e.amount());
    }   // commit → then the listener returns → Kafka offset committed
}
// Trade-off: the gateway call happens while a DB transaction (and connection) is open —
// fine for a short call with a tight timeout. For slow providers, split it: record
// PENDING, call, then record the outcome, using the event id as the idempotency key
// throughout. If anything throws, the transaction rolls back (claim included), so the
// redelivered record is processed again — and the gateway's idempotency key returns the
// original charge.

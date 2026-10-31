@Service
class Charges {
    private final PaymentRepository payments;
    private final PspClient psp;                 // HTTP client with connect/read timeouts

    public Payment charge(UUID paymentId) {
        Payment p = payments.get(paymentId);
        try {
            PspResult r = psp.capture(p.pspToken(), p.amount(), p.currency(),
                                      /* idempotency key */ p.id().toString());
            return payments.transition(p.id(), r.succeeded() ? State.CAPTURED : State.DECLINED, r.reference());
        } catch (PspTimeoutException | PspUnavailableException unknown) {
            // Unknown outcome: never FAILED, never a retry with a new key.
            payments.transition(p.id(), State.PENDING_CONFIRMATION, null);
            // query PSP by our key if no webhook
            confirmations.scheduleLookup(p.id(), Duration.ofSeconds(30));
            return payments.get(p.id());
        }
    }
}

@RestController
class PspWebhookController {
    // HMAC signature over raw body + timestamp tolerance
    private final WebhookVerifier verifier;
    private final JdbcTemplate jdbc;
    private final PaymentStateMachine machine;

    @PostMapping("/webhooks/psp")
    @Transactional
    public ResponseEntity<Void> receive(@RequestHeader("X-Signature") String sig, @RequestBody byte[] body) {
        // 400 if the signature is bad
        PspEvent e = verifier.verifyAndParse(sig, body);
        int fresh = jdbc.update("INSERT INTO processed_psp_events (event_id) VALUES (?) ON CONFLICT DO NOTHING", e.id());
        // duplicate delivery
        if (fresh == 0) return ResponseEntity.ok().build();

        // Applies only valid transitions; writes ledger entries and an outbox row in this
        // same transaction.
        machine.apply(e.paymentId(), e.type(), e.amount(), e.reference());
        // 2xx tells the PSP to stop retrying
        return ResponseEntity.ok().build();
    }
}

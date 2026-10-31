// Receiver side (Spring): verify, de-duplicate, acknowledge fast, process asynchronously.
@PostMapping("/webhooks/payments")
// raw bytes: re-serialised JSON won't match
ResponseEntity<Void> receive(
        @RequestBody byte[] rawBody,
        @RequestHeader("Webhook-Signature") String signature,
        @RequestHeader("Webhook-Id") String eventId)
        throws Exception {
    String body = new String(rawBody, StandardCharsets.UTF_8);
    if (!verify(body, signature, secret, Instant.now().getEpochSecond(), 300))
        return ResponseEntity.status(401).build();

    int inserted =
            jdbc.update(
                    "INSERT INTO webhook_inbox (event_id, body) VALUES (?, ?::jsonb) ON"
                        + " CONFLICT DO NOTHING",
                    eventId,
                    body);
    // duplicates (retries) are acknowledged but not re-processed
    if (inserted == 1) queue.submit(eventId);
    // answer within seconds; heavy work happens later
    return ResponseEntity.ok().build();
}

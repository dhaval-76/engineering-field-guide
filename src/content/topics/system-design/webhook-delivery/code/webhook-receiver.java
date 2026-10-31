// Receiver side (Spring): verify, de-duplicate, acknowledge fast, process asynchronously.
@PostMapping("/webhooks/payments")
ResponseEntity<Void> receive(@RequestBody byte[] rawBody,                       // raw bytes: re-serialised JSON won't match
                             @RequestHeader("Webhook-Signature") String signature,
                             @RequestHeader("Webhook-Id") String eventId) throws Exception {
    String body = new String(rawBody, StandardCharsets.UTF_8);
    if (!verify(body, signature, secret, Instant.now().getEpochSecond(), 300))
        return ResponseEntity.status(401).build();

    int inserted = jdbc.update(
            "INSERT INTO webhook_inbox (event_id, body) VALUES (?, ?::jsonb) ON CONFLICT DO NOTHING", eventId, body);
    if (inserted == 1) queue.submit(eventId);          // duplicates (retries) are acknowledged but not re-processed
    return ResponseEntity.ok().build();                // answer within seconds; heavy work happens later
}

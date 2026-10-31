/**
 * Sender: sign "timestamp.body" so receivers can verify origin, integrity and freshness.
 */
static String signatureHeader(String body, long timestamp, byte[] secret) throws Exception {
    return "t=" + timestamp + ",v1=" + hmacHex(timestamp + "." + body, secret);
}

/**
 * Receiver: recompute over the RAW body bytes, compare in constant time, reject old
 * timestamps (replays).
 */
static boolean verify(String body, String header, byte[] secret, long now, long toleranceSeconds) throws Exception {
    long t = -1; String v1 = null;
    for (String part : header.split(",")) {
        if (part.startsWith("t=")) t = Long.parseLong(part.substring(2));
        else if (part.startsWith("v1=")) v1 = part.substring(3);
    }
    if (t < 0 || v1 == null || Math.abs(now - t) > toleranceSeconds) return false;
    String expected = hmacHex(t + "." + body, secret);
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
}

static String hmacHex(String data, byte[] secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
}

/** Retry schedule: exponential backoff with full jitter, capped, for about three days. */
static long nextDelaySeconds(int attempt, Random random) {
    // never wait more than 6 h between tries
    long cap = 6 * 3600;
    long base = Math.min(cap, 30L << Math.min(attempt, 20));    // 30 s, 1 m, 2 m, 4 m, …
    // jitter spreads retries out
    return base / 2 + (long) (random.nextDouble() * base / 2);
}

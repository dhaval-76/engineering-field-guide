/**
 * Signed URLs let the CDN serve private files (paid videos, invoices) without calling the
 * origin.
 */
static String sign(String path, long expiresEpochSeconds, byte[] secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    byte[] sig =
            mac.doFinal(
                    (path + "?expires=" + expiresEpochSeconds)
                            .getBytes(StandardCharsets.UTF_8));
    return path
            + "?expires="
            + expiresEpochSeconds
            + "&sig="
            + Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
}

/** Runs at the edge: reject expired or tampered links. */
static boolean verify(
        String path, long expires, String sig, byte[] secret, long nowEpochSeconds)
        throws Exception {
    if (nowEpochSeconds > expires) return false;
    String expected = sign(path, expires, secret);
    String given = path + "?expires=" + expires + "&sig=" + sig;
    // constant-time compare
    return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
}

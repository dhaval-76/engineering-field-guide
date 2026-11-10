static final long TOLERANCE_SECONDS = 300; // reject anything older: a replay

/** Stripe-style header "t=<unix seconds>,v1=<hex HMAC-SHA256 of t.body>". */
String signatureHeader(String secret, long timestamp, String body) throws GeneralSecurityException {
    return "t=" + timestamp + ",v1=" + hmacHex(secret, timestamp + "." + body);
}

/** body must be the raw request body exactly as received, not re-serialised JSON. */
boolean verify(String secret, String header, String body, long nowSeconds) {
    try {
        long timestamp = -1;
        List<String> signatures = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) timestamp = Long.parseLong(kv[1]);
            else if (kv[0].equals("v1")) signatures.add(kv[1]); // ignore other schemes
        }
        if (timestamp < 0 || Math.abs(nowSeconds - timestamp) > TOLERANCE_SECONDS) return false;
        byte[] expected = hmacHex(secret, timestamp + "." + body).getBytes(StandardCharsets.US_ASCII);
        for (String s : signatures) { // several during a secret rotation
            if (MessageDigest.isEqual(expected, s.getBytes(StandardCharsets.US_ASCII))) return true;
        }
        return false;
    } catch (NumberFormatException | GeneralSecurityException e) {
        return false;
    }
}

String hmacHex(String secret, String message) throws GeneralSecurityException {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
}

static final int ITERATIONS = 600_000; // OWASP's minimum for PBKDF2-HMAC-SHA256

/** Returns "pbkdf2_sha256$iterations$salt$hash": everything needed to check it later. */
String hashPassword(char[] password) throws GeneralSecurityException {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt); // unique per password, so equal passwords differ
    byte[] hash = pbkdf2(password, salt, ITERATIONS);
    Base64.Encoder b64 = Base64.getEncoder();
    return "pbkdf2_sha256$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$"
            + b64.encodeToString(hash);
}

boolean verifyPassword(char[] password, String stored) throws GeneralSecurityException {
    String[] parts = stored.split("\\$");
    byte[] salt = Base64.getDecoder().decode(parts[2]);
    byte[] expected = Base64.getDecoder().decode(parts[3]);
    byte[] actual = pbkdf2(password, salt, Integer.parseInt(parts[1]));
    return MessageDigest.isEqual(actual, expected); // constant time: no timing leak
}

byte[] pbkdf2(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
}

// Store password HASHES made with a slow, salted algorithm — never plain SHA-256, never
// encryption.
@Bean
PasswordEncoder passwordEncoder() {
    // Stores "{bcrypt}$2a$10$…" — the {id} prefix lets you move to argon2 later and
    // rehash on login
    // default bcrypt; argon2/scrypt available
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}

boolean login(String email, String password) {
    User u = users.findByEmail(email).orElse(null);
    // same work when the user doesn't exist
    String hash = (u != null) ? u.passwordHash() : DUMMY_HASH;
    // constant-time comparison inside
    boolean ok = encoder.matches(password, hash);
    if (ok && encoder.upgradeEncoding(hash)) users.updateHash(u.id(), encoder.encode(password));
    return ok && u != null;
}
// Plus: rate-limit and lock out per account and per IP, check passwords against breach
// lists, offer MFA / passkeys (WebAuthn), and return the same error for "no such user"
// and "wrong password".

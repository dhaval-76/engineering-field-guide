// Store password HASHES made with a slow, salted algorithm — never plain SHA-256, never encryption.
@Bean
PasswordEncoder passwordEncoder() {
    // Stores "{bcrypt}$2a$10$…" — the {id} prefix lets you move to argon2 later and rehash on login
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // default bcrypt; argon2/scrypt available
}

boolean login(String email, String password) {
    User u = users.findByEmail(email).orElse(null);
    String hash = (u != null) ? u.passwordHash() : DUMMY_HASH;   // same work when the user doesn't exist
    boolean ok = encoder.matches(password, hash);                // constant-time comparison inside
    if (ok && encoder.upgradeEncoding(hash)) users.updateHash(u.id(), encoder.encode(password));
    return ok && u != null;
}
// Plus: rate-limit and lock out per account and per IP, check passwords against breach lists,
// offer MFA / passkeys (WebAuthn), and return the same error for "no such user" and "wrong password".

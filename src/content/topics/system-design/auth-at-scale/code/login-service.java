@Service
class LoginService {
    private final PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    // A real hash of a random password: verifying against it costs the same as a real
    // account.
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString());
    private final UserRepository users;
    private final LoginRateLimits limits;       // Redis: per IP and per account
    private final Sessions sessions;

    public LoginResult login(String email, String password, String ip) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!limits.tryIp(ip) || !limits.tryAccount(normalized)) throw new TooManyAttemptsException();

        Optional<User> user = users.findByEmail(normalized);
        // Always run the slow hash, so response time doesn't reveal whether the account
        // exists.
        boolean ok = encoder.matches(password, user.map(User::passwordHash).orElse(dummyHash));
        if (user.isEmpty() || !ok) {
            limits.recordFailure(normalized, ip);
            // one message for both cases
            throw new BadCredentialsException("Invalid email or password");
        }
        limits.reset(normalized);
        if (user.get().mfaEnabled()) return LoginResult.mfaRequired(sessions.pendingMfa(user.get()));
        return LoginResult.success(sessions.create(user.get(), ip));
    }
}

@Service
class PasswordResets {
    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate jdbc;
    // users, mailer, sessions and encoder are injected like the fields above

    // always responds "if it exists, we sent a link"
    public void request(String email) {
        users.findByEmail(email).ifPresent(u -> {
            byte[] raw = new byte[32];
            random.nextBytes(raw);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            jdbc.update("INSERT INTO password_resets (token_hash, user_id, expires_at) VALUES (?, ?, now() + interval '15 minutes')",
                        sha256(token), u.id());       // only the hash is stored
            mailer.sendReset(u.email(), token);
        });
    }

    @Transactional
    public void complete(String token, String newPassword) {
        // Atomic single-use claim: two concurrent submissions can't both succeed.
        Long userId = jdbc.query("""
            UPDATE password_resets SET used_at = now()
            WHERE token_hash = ? AND used_at IS NULL AND expires_at > now()
            RETURNING user_id""", rs -> rs.next() ? rs.getLong(1) : null, sha256(token));
        if (userId == null) throw new InvalidResetTokenException();
        users.updatePasswordHash(userId, encoder.encode(newPassword));
        // log out everywhere after a reset
        sessions.revokeAll(userId);
    }
}

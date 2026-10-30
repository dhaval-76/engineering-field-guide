// Refresh tokens: random (not JWT), stored HASHED, single-use, rotated, and grouped into a family.
@Entity @Table(name = "refresh_tokens")
class RefreshToken {
    @Id UUID id;
    String userId;
    UUID familyId;               // all tokens descended from one login
    String tokenHash;            // SHA-256 of the token; a DB leak doesn't leak usable tokens
    Instant expiresAt;
    Instant usedAt;              // null until rotated
    Instant revokedAt;
}

@Transactional
public TokenPair refresh(String presented) {
    RefreshToken rt = repo.findByTokenHash(sha256(presented)).orElseThrow(InvalidGrant::new);

    if (rt.revokedAt != null || rt.expiresAt.isBefore(Instant.now())) throw new InvalidGrant();

    // Atomically mark it used; 0 rows means it was already used → possible theft (reuse detection)
    int rotated = repo.markUsedIfUnused(rt.id, Instant.now());   // UPDATE … SET used_at=? WHERE id=? AND used_at IS NULL
    if (rotated == 0) {
        repo.revokeFamily(rt.familyId);                          // log out every session from that login
        throw new InvalidGrant();
    }

    String nextRefresh = randomToken(32);                        // SecureRandom, base64url
    repo.save(RefreshToken.next(rt, sha256(nextRefresh), Instant.now().plus(Duration.ofDays(14))));
    return new TokenPair(tokens.issueAccessToken(rt.userId, Duration.ofMinutes(10)), nextRefresh);
}

public void logout(String presented) {
    repo.findByTokenHash(sha256(presented)).ifPresent(rt -> repo.revokeFamily(rt.familyId));
}

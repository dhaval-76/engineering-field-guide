// Refresh tokens: random (not JWT), stored HASHED, single-use, rotated, and grouped into
// a family.
@Entity @Table(name = "refresh_tokens")
class RefreshToken {
    @Id UUID id;
    String userId;
    UUID familyId;               // all tokens descended from one login
    // SHA-256 of the token; a DB leak doesn't leak usable tokens
    String tokenHash;
    Instant expiresAt;
    Instant usedAt;              // null until rotated
    Instant revokedAt;
}

@Transactional
public TokenPair refresh(String presented) {
    RefreshToken rt = repo.findByTokenHash(sha256(presented)).orElseThrow(InvalidGrant::new);

    if (rt.revokedAt != null || rt.expiresAt.isBefore(Instant.now())) throw new InvalidGrant();

    // Atomically mark it used; 0 rows means it was already used → possible theft (reuse
    // detection)
    // UPDATE … SET used_at=? WHERE id=? AND used_at IS NULL
    int rotated = repo.markUsedIfUnused(rt.id, Instant.now());
    if (rotated == 0) {
        // log out every session from that login
        repo.revokeFamily(rt.familyId);
        throw new InvalidGrant();
    }

    // SecureRandom, base64url
    String nextRefresh = randomToken(32);
    repo.save(RefreshToken.next(rt, sha256(nextRefresh), Instant.now().plus(Duration.ofDays(14))));
    return new TokenPair(tokens.issueAccessToken(rt.userId, Duration.ofMinutes(10)), nextRefresh);
}

public void logout(String presented) {
    repo.findByTokenHash(sha256(presented)).ifPresent(rt -> repo.revokeFamily(rt.familyId));
}

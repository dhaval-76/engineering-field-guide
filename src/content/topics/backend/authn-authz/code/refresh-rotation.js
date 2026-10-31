// Refresh tokens: random (not JWT), stored HASHED, single-use, rotated, grouped into a
// family per login.
import { createHash, randomBytes, randomUUID } from 'node:crypto';

const sha256 = (s) => createHash('sha256').update(s).digest('hex');
export class InvalidGrant extends Error {}

// repo: findByTokenHash, markUsedIfUnused, revokeFamily, insert — backed by a
// refresh_tokens table
export async function refresh(repo, tokens, presented, now = new Date()) {
  const rt = await repo.findByTokenHash(sha256(presented));
  if (!rt || rt.revokedAt || rt.expiresAt <= now) throw new InvalidGrant();

  // UPDATE refresh_tokens SET used_at = $2 WHERE id = $1 AND used_at IS NULL  → rows
  // changed
  if ((await repo.markUsedIfUnused(rt.id, now)) === 0) {
    // reuse of an old token ⇒ possible theft: log out that login everywhere
    await repo.revokeFamily(rt.familyId);
    throw new InvalidGrant();
  }

  const next = randomBytes(32).toString('base64url');
  await repo.insert({
    id: randomUUID(), userId: rt.userId, familyId: rt.familyId, tokenHash: sha256(next),
    expiresAt: new Date(now.getTime() + 14 * 24 * 3600 * 1000), usedAt: null, revokedAt: null,
  });
  return { accessToken: await tokens.issueAccessToken(rt.userId, { minutes: 10 }), refreshToken: next };
}

export async function logout(repo, presented) {
  const rt = await repo.findByTokenHash(sha256(presented));
  if (rt) await repo.revokeFamily(rt.familyId);
}

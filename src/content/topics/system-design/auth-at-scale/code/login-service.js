// Login with uniform timing and messages, rate limits, MFA; password resets with
// single-use hashed tokens. hashPassword/verifyPassword from Authentication and
// authorization (scrypt) — argon2 works the same way.
import { createHash, randomBytes } from 'node:crypto';
const sha256 = (s) => createHash('sha256').update(s).digest('hex');
// a real hash: same cost as a real account
const DUMMY_HASH = hashPassword(randomBytes(16).toString('hex'));

export async function login(email, password, ip) {
  const normalized = email.trim().toLowerCase();
  if (!(await limits.tryIp(ip)) || !(await limits.tryAccount(normalized))) throw httpError(429, 'too many attempts');
  const user = await users.findByEmail(normalized);
  // always run the slow hash
  const ok = verifyPassword(password, user?.passwordHash ?? DUMMY_HASH);
  if (!user || !ok) {
    await limits.recordFailure(normalized, ip);
    throw httpError(401, 'Invalid email or password'); // one message for both cases
  }
  await limits.reset(normalized);
  return user.mfaEnabled ? { mfaRequired: await sessions.pendingMfa(user) } : { session: await sessions.create(user, ip) };
}

// always answers "if it exists, we sent a link"
export async function requestReset(email) {
  const user = await users.findByEmail(email.trim().toLowerCase());
  if (!user) return;
  const token = randomBytes(32).toString('base64url');
  await pool.query(`INSERT INTO password_resets (token_hash, user_id, expires_at) VALUES ($1, $2, now() + interval '15 minutes')`,
    [sha256(token), user.id]);                // only the hash is stored
  await mailer.sendReset(user.email, token);
}

export async function completeReset(token, newPassword) {
  const { rows } = await pool.query(          // atomic single-use claim
    `UPDATE password_resets SET used_at = now()
     WHERE token_hash = $1 AND used_at IS NULL AND expires_at > now() RETURNING user_id`, [sha256(token)]);
  if (!rows.length) throw httpError(400, 'invalid or expired link');
  await users.updatePasswordHash(rows[0].user_id, hashPassword(newPassword));
  await sessions.revokeAll(rows[0].user_id);  // log out everywhere after a reset
}

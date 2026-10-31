// Store password HASHES from a slow, salted algorithm — never plain SHA-256, never
// encryption. Node's built-in scrypt; the argon2 package (argon2id) is an equally good
// choice.
import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';

const PARAMS = { N: 2 ** 15, r: 8, p: 1, maxmem: 64 * 1024 * 1024 };

export function hashPassword(password) {
  const salt = randomBytes(16);
  const hash = scryptSync(password.normalize('NFKC'), salt, 32, PARAMS);
  // params stored → upgradable
  return `scrypt$${PARAMS.N}$${PARAMS.r}$${PARAMS.p}$${salt.toString('base64')}$${hash.toString('base64')}`;
}

export function verifyPassword(password, stored) {
  const [, N, r, p, salt, hash] = stored.split('$');
  const expected = Buffer.from(hash, 'base64');
  const actual = scryptSync(password.normalize('NFKC'), Buffer.from(salt, 'base64'), expected.length, {
    N: +N, r: +r, p: +p, maxmem: 64 * 1024 * 1024,
  });
  return timingSafeEqual(actual, expected); // constant-time comparison
}

const DUMMY_HASH = hashPassword('not-a-real-password');

export async function login(users, email, password) {
  const u = await users.findByEmail(email);
  // same work when the user doesn't exist
  const ok = verifyPassword(password, u ? u.passwordHash : DUMMY_HASH);
  return ok && u ? u : null; // one error message for "no such user" and "wrong password"
}
// Plus: rate-limit and lock out per account and per IP, check breached-password lists,
// offer MFA / passkeys.

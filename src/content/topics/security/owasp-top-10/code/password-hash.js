import { pbkdf2Sync, randomBytes, timingSafeEqual } from 'node:crypto';

const ITERATIONS = 600_000; // OWASP's minimum for PBKDF2-HMAC-SHA256

/** Returns "pbkdf2_sha256$iterations$salt$hash": everything needed to check it later. */
function hashPassword(password) {
  const salt = randomBytes(16); // unique per password, so equal passwords differ
  const hash = pbkdf2Sync(password, salt, ITERATIONS, 32, 'sha256');
  return `pbkdf2_sha256$${ITERATIONS}$${salt.toString('base64')}$${hash.toString('base64')}`;
}

function verifyPassword(password, stored) {
  const [, iterations, salt, expected] = stored.split('$');
  const actual = pbkdf2Sync(password, Buffer.from(salt, 'base64'), Number(iterations), 32, 'sha256');
  return timingSafeEqual(actual, Buffer.from(expected, 'base64')); // constant time
}

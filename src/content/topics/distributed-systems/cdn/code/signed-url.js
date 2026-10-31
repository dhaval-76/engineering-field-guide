import { createHmac, timingSafeEqual } from 'node:crypto';

// Signed URLs let the CDN serve private files (paid videos, invoices) without calling the
// origin.
export function sign(path, expiresEpochSeconds, secret) {
  const sig = createHmac('sha256', secret).update(`${path}?expires=${expiresEpochSeconds}`).digest('base64url');
  return `${path}?expires=${expiresEpochSeconds}&sig=${sig}`;
}

// Runs at the edge: reject expired or tampered links.
export function verify(path, expires, sig, secret, nowEpochSeconds) {
  if (nowEpochSeconds > expires) return false;
  const expected = Buffer.from(sign(path, expires, secret));
  const given = Buffer.from(`${path}?expires=${expires}&sig=${sig}`);
  return expected.length === given.length && timingSafeEqual(expected, given);
}

import { createHmac, timingSafeEqual } from 'node:crypto';

const hmacHex = (data, secret) => createHmac('sha256', secret).update(data).digest('hex');

// Sender: sign "timestamp.body".
export const signatureHeader = (body, timestamp, secret) => `t=${timestamp},v1=${hmacHex(`${timestamp}.${body}`, secret)}`;

// Receiver: verify the RAW body (before JSON parsing), constant-time compare, reject stale timestamps.
export function verify(body, header, secret, now, toleranceSeconds = 300) {
  const parts = Object.fromEntries(header.split(',').map((p) => p.split('=', 2)));
  const t = Number(parts.t);
  if (!Number.isFinite(t) || !parts.v1 || Math.abs(now - t) > toleranceSeconds) return false;
  const expected = Buffer.from(hmacHex(`${t}.${body}`, secret));
  const given = Buffer.from(parts.v1);
  return expected.length === given.length && timingSafeEqual(expected, given);
}

// Exponential backoff with jitter, capped at 6 hours between attempts.
export function nextDelaySeconds(attempt, random = Math.random) {
  const base = Math.min(6 * 3600, 30 * 2 ** Math.min(attempt, 20));
  return Math.floor(base / 2 + random() * (base / 2));
}

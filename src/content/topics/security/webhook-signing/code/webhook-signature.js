import { createHmac, timingSafeEqual } from 'node:crypto';

const TOLERANCE_SECONDS = 300; // reject anything older: a replay

/** Stripe-style header "t=<unix seconds>,v1=<hex HMAC-SHA256 of t.body>". */
function signatureHeader(secret, timestamp, body) {
  return `t=${timestamp},v1=${hmacHex(secret, `${timestamp}.${body}`)}`;
}

/** body must be the raw request body exactly as received, not re-serialised JSON. */
function verify(secret, header, body, nowSeconds) {
  let timestamp = -1;
  const signatures = [];
  for (const part of header.split(',')) {
    const [key, value] = part.trim().split(/=(.*)/s);
    if (key === 't' && /^\d+$/.test(value)) timestamp = Number(value);
    else if (key === 'v1') signatures.push(value); // ignore other schemes
  }
  if (timestamp < 0 || Math.abs(nowSeconds - timestamp) > TOLERANCE_SECONDS) return false;
  const expected = Buffer.from(hmacHex(secret, `${timestamp}.${body}`));
  return signatures.some((s) => {
    const given = Buffer.from(s); // several during a secret rotation
    return given.length === expected.length && timingSafeEqual(given, expected);
  });
}

function hmacHex(secret, message) {
  return createHmac('sha256', secret).update(message).digest('hex');
}

// Node 18+: fetch with a deadline and an idempotency key
const PAYMENTS = process.env.PAYMENTS_URL ?? 'http://payments';

export async function charge(orderId, amount, { signal } = {}) {
  const res = await fetch(`${PAYMENTS}/api/payments`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'idempotency-key': `order-${orderId}`,          // safe to retry
    },
    body: JSON.stringify({ orderId, amount }),
    // whichever comes first: our own 2 s timeout or the caller's cancellation
    signal: AbortSignal.any([AbortSignal.timeout(2000), ...(signal ? [signal] : [])]),
  });
  if (!res.ok) throw new PaymentsError(res.status, await res.text());
  return res.json();
}

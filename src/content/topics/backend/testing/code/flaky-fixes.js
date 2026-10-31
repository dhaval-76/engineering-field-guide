import { it, expect, vi } from 'vitest';
import { randomUUID } from 'node:crypto';

// 1. Time: production code takes a `now` function instead of calling Date.now() directly.
export const isExpired = (trial, now = () => new Date()) => now() > trial.endsAt;

it('expires after the end time', () => {
  const fixed = () => new Date('2026-03-15T00:00:01Z');
  // same result every run
  expect(isExpired({ endsAt: new Date('2026-03-15T00:00:00Z') }, fixed)).toBe(true);
});
// (Or vi.useFakeTimers() + vi.setSystemTime(...) for code you can't change.)

// 2. Async: wait for the condition, not for an arbitrary number of milliseconds.
it('eventually sends the confirmation email', async () => {
  await service.placeOrder(request);
  await vi.waitFor(() => expect(mailbox.sentTo('c1@example.com')).toHaveLength(1), { timeout: 5000 });
  // ✗ await sleep(500) — too short on a busy CI machine, wasted time everywhere else
});

// 3. Shared state: each test creates the data it needs, with unique keys.
it('allows only one active cart per customer', async () => {
  // no clash with other tests
  const customer = await customers.create(`user-${randomUUID()}@example.com`);
  await carts.open(customer);
  await expect(carts.open(customer)).rejects.toThrow('active cart exists');
});

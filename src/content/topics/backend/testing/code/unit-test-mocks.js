// Vitest (Jest has the same API with jest.fn())
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { OrderService, PaymentDeclinedError } from './order-service.js';

describe('OrderService.placeOrder', () => {
  let orders, payments, events, service;
  const request = { customerId: 'C-1', items: [{ sku: 'BOOK', qty: 2 }] };

  beforeEach(() => {
    // Plain objects with mock functions — dependencies are passed in, not imported.
    orders = { save: vi.fn(async (order) => ({ ...order, id: 'O-1' })) };
    payments = { charge: vi.fn() };
    events = { publish: vi.fn() };
    service = new OrderService({ orders, payments, events, now: () => new Date('2026-03-01T10:00:00Z') });
  });

  it('charges, saves and publishes', async () => {
    payments.charge.mockResolvedValue({ id: 'ch_1', status: 'succeeded' });

    const result = await service.placeOrder(request);

    expect(result.status).toBe('PAID');
    expect(orders.save).toHaveBeenCalledWith(expect.objectContaining({ customerId: 'C-1' }));
    expect(events.publish).toHaveBeenCalledWith({ type: 'OrderPlaced', orderId: 'O-1' });
  });

  it('saves nothing when the payment is declined', async () => {
    payments.charge.mockRejectedValue(new PaymentDeclinedError('card_declined'));

    await expect(service.placeOrder(request)).rejects.toThrow(PaymentDeclinedError);
    expect(orders.save).not.toHaveBeenCalled();
    expect(events.publish).not.toHaveBeenCalled();
  });
});

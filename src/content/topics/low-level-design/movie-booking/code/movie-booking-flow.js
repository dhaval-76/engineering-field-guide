// uses ShowSeat from above
const HOLD_TTL_MS = 8 * 60 * 1000;

export class BookingService {
  constructor({ seats, payments, bookings, now = () => Date.now() }) {
    Object.assign(this, { seats, payments, bookings, now }); // seats.get(showId, seatId) → ShowSeat
  }

  // Step 1 — hold all requested seats or none.
  hold(showId, seatIds, userId) {
    if (seatIds.length === 0 || seatIds.length > 10) throw new Error('1–10 seats');
    const holdId = crypto.randomUUID();
    const taken = [];
    for (const seatId of [...seatIds].sort()) {
      const s = this.seats.get(showId, seatId);
      if (!s.tryHold(holdId, this.now(), HOLD_TTL_MS)) {
        taken.forEach((t) => t.release(holdId)); // roll back the partial hold
        throw new Error(`seat ${seatId} unavailable`);
      }
      taken.push(s);
    }
    return { id: holdId, showId, userId, seatIds: [...seatIds], expiresAt: this.now() + HOLD_TTL_MS };
  }

  // Step 2 — pay (idempotently, keyed by the hold), then confirm. The await is where other
  // requests interleave: the hold may expire while the user pays.
  async checkout(hold, amount, method) {
    const result = await this.payments.charge(amount, method, { idempotencyKey: hold.id });
    if (!result.succeeded) {
      this.release(hold);
      throw new Error(`payment failed: ${result.reason}`);
    }
    const allConfirmed = hold.seatIds.every((id) => this.seats.get(hold.showId, id).confirm(hold.id, this.now()));
    if (!allConfirmed) {
      await this.payments.refund(result.paymentId, 'hold expired');
      this.release(hold);
      throw new Error(`hold ${hold.id} expired`);
    }
    return this.bookings.save({ ...hold, paymentId: result.paymentId, status: 'CONFIRMED' });
  }

  release(hold) {
    hold.seatIds.forEach((id) => this.seats.get(hold.showId, id).release(hold.id));
  }
}

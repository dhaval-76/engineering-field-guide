// Static catalogue (rarely changes): movies, theatres, screens with seat layouts. A show
// is a movie on a screen at a time. Availability lives on ShowSeat — the same physical
// seat is free for the 6 pm show and booked for the 9 pm show.

export class ShowSeat {
  status = 'AVAILABLE'; // AVAILABLE | HELD | BOOKED
  #holdId = null;
  #holdExpiresAt = 0;

  constructor(showId, seatId) {
    Object.assign(this, { showId, seatId });
  }

  tryHold(holdId, now, ttlMs) {
    // lazy expiry
    if (this.status === 'HELD' && this.#holdExpiresAt <= now) this.status = 'AVAILABLE';
    if (this.status !== 'AVAILABLE') return false;
    this.status = 'HELD';
    this.#holdId = holdId;
    this.#holdExpiresAt = now + ttlMs;
    return true;
  }

  confirm(holdId, now) {
    if (this.status !== 'HELD' || this.#holdId !== holdId || this.#holdExpiresAt <= now) return false;
    this.status = 'BOOKED';
    return true;
  }

  release(holdId) {
    if (this.status === 'HELD' && this.#holdId === holdId) {
      this.status = 'AVAILABLE';
      this.#holdId = null;
    }
  }
}

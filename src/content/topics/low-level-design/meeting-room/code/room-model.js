// Half-open intervals [start, end) in epoch ms.
export const overlaps = (a, b) => a.start < b.end && b.start < a.end;

export class SlotTakenError extends Error {
  constructor(roomId, slot) {
    super(`room ${roomId} is taken for that slot`);
    this.slot = slot;
  }
}

export class Room {
  #bookings = []; // kept sorted by start

  constructor({ id, capacity, amenities = [] }) {
    Object.assign(this, { id, capacity, amenities: new Set(amenities) });
  }

  isFree(slot) {
    // binary search for the first booking starting at/after slot.start
    let lo = 0, hi = this.#bookings.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (this.#bookings[mid].slot.start < slot.start) lo = mid + 1; else hi = mid;
    }
    const before = this.#bookings[lo - 1], after = this.#bookings[lo];
    return !(before && overlaps(before.slot, slot)) && !(after && overlaps(after.slot, slot));
  }

  book(slot, organizer, attendees = [], seriesId = null) {
    if (!this.isFree(slot)) throw new SlotTakenError(this.id, slot);
    const booking = { id: crypto.randomUUID(), roomId: this.id, slot, organizer, attendees, seriesId };
    this.#bookings.push(booking);
    this.#bookings.sort((a, b) => a.slot.start - b.slot.start);
    return booking;
  }
}

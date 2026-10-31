// Which spot sizes a vehicle may use, smallest first (so cars don't take truck spots unnecessarily).
export const FITS_IN = {
  MOTORCYCLE: ['SMALL', 'MEDIUM', 'LARGE'],
  CAR: ['MEDIUM', 'LARGE'],
  TRUCK: ['LARGE'],
};

export class ParkingSpot {
  #occupant = null;
  constructor(id, floor, size) {
    Object.assign(this, { id, floor, size });
  }
  // Single-threaded Node: check-and-set can't interleave. Across processes this becomes a DB/Redis claim.
  tryOccupy(vehicle) {
    if (this.#occupant) return false;
    this.#occupant = vehicle;
    return true;
  }
  release() {
    this.#occupant = null;
  }
  get isFree() {
    return this.#occupant === null;
  }
}

export class Ticket {
  status = 'ACTIVE'; // ACTIVE → PAID → CLOSED
  fee = null;
  constructor(id, vehicle, spot, entry) {
    Object.assign(this, { id, vehicle, spot, entry });
  }
  markPaid(fee) {
    if (this.status !== 'ACTIVE') throw new Error(`ticket ${this.id} is ${this.status}`);
    Object.assign(this, { fee, status: 'PAID' });
  }
  close() {
    if (this.status !== 'PAID') throw new Error('pay before exit');
    this.status = 'CLOSED';
  }
}

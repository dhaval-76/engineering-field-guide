const FITS = {
  MOTORCYCLE: ['SMALL', 'MEDIUM', 'LARGE'],
  CAR: ['MEDIUM', 'LARGE'],
  TRUCK: ['LARGE'],
};

export class ParkingLot {
  #spots; #pricing; #now; #active = new Map(); #listeners = new Set();

  constructor({ spots, pricing, now = () => Date.now() }) {
    this.#spots = spots.map((s) => ({ ...s, occupant: null }));
    this.#pricing = pricing;
    this.#now = now;
  }

  onSpotChange(fn) {
    this.#listeners.add(fn);
    return () => this.#listeners.delete(fn);
  }

  // Node runs this synchronously on one thread, so "find a free spot and claim it" can't
  // interleave with another request. (With multiple processes the claim must move to a
  // database or Redis.)
  park(vehicle) {
    const fits = FITS[vehicle.type];
    const spot = this.#spots
      .filter((s) => s.occupant === null && fits.includes(s.size))
      .sort((a, b) => a.floor - b.floor || fits.indexOf(a.size) - fits.indexOf(b.size))[0];
    if (!spot) return null;
    spot.occupant = vehicle;
    const ticket = { id: crypto.randomUUID(), vehicle, spot, entry: this.#now(), status: 'ACTIVE' };
    this.#active.set(ticket.id, ticket);
    this.#listeners.forEach((fn) => fn(spot, false));
    return ticket;
  }

  pay(ticketId, amount) {
    const t = this.#get(ticketId);
    if (t.status !== 'ACTIVE') throw new Error(`ticket is ${t.status}`);
    const due = this.#pricing(t, this.#now());
    if (amount < due) throw new Error(`due ${due}`);
    Object.assign(t, { status: 'PAID', fee: due });
  }

  exit(ticketId) {
    const t = this.#get(ticketId);
    if (t.status !== 'PAID') throw new Error('pay before exit');
    t.status = 'CLOSED';
    this.#active.delete(ticketId);
    t.spot.occupant = null;
    this.#listeners.forEach((fn) => fn(t.spot, true));
  }

  #get(id) {
    const t = this.#active.get(id);
    if (!t) throw new Error(`unknown ticket ${id}`);
    return t;
  }
}

// Pricing is a plain function (strategy):
export const hourlyWithCap = (rates, caps) => (ticket, now) => {
  const hours = Math.ceil(Math.max(1, (now - ticket.entry) / 60000) / 60);
  const days = Math.floor(hours / 24);
  const rest = Math.min((hours % 24) * rates[ticket.vehicle.type], caps[ticket.vehicle.type]);
  return days * caps[ticket.vehicle.type] + rest;
};

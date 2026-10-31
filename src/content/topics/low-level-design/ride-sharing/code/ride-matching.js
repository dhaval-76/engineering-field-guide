// uses GridIndex and distanceKm from above
export class Driver {
  status = 'OFFLINE'; // OFFLINE | AVAILABLE | OFFERED | ON_TRIP
  location = null;
  constructor(id, vehicle, rating) {
    Object.assign(this, { id, vehicle, rating });
  }
  // Only one ride can reserve a driver. Synchronous check-and-set is atomic on Node's
  // event loop; across servers this becomes a Redis SET NX or a conditional DB update.
  tryReserve() {
    if (this.status !== 'AVAILABLE') return false;
    this.status = 'OFFERED';
    return true;
  }
  release() {
    this.status = 'AVAILABLE';
  }
}

// Strategy: closer is better, rating breaks near-ties.
export const nearestGoodDriver = (ride, candidates) =>
  [...candidates].sort((a, b) => distanceKm(a.location, ride.pickup) - 0.3 * a.rating - (distanceKm(b.location, ride.pickup) - 0.3 * b.rating));

export class Dispatcher {
  constructor({ index, strategy, offers }) {
    // offers.offerAndAwait(driver, ride, ms) → Promise<boolean>
    Object.assign(this, { index, strategy, offers });
  }

  // Widen the radius until someone accepts or we give up.
  async match(ride, wanted) {
    for (const radius of [1, 2, 4, 8]) {
      const candidates = this.index.near(ride.pickup, radius).filter((d) => d.vehicle === wanted && d.status === 'AVAILABLE');
      for (const d of this.strategy(ride, candidates)) {
        // another ride got this driver first
        if (!d.tryReserve()) continue;
        if (await this.offers.offerAndAwait(d, ride, 15_000)) {
          ride.driverId = d.id;
          return d;
        }
        d.release();                                       // declined or timed out
      }
    }
    return null;
  }
}

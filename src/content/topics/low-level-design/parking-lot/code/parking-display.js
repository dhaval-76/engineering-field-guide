// Keeps per-floor, per-size free counts; updated by spot events, never by scanning.
export class DisplayBoard {
  #free = new Map(); // "floor:size" -> count

  constructor(spots) {
    for (const s of spots) if (s.isFree) this.#bump(s.floor, s.size, 1);
  }

  spotChanged(spot, nowFree) {
    this.#bump(spot.floor, spot.size, nowFree ? 1 : -1);
    this.render(spot.floor);
  }

  freeOn(floor, size) {
    return this.#free.get(`${floor}:${size}`) ?? 0;
  }

  #bump(floor, size, by) {
    const k = `${floor}:${size}`;
    this.#free.set(k, (this.#free.get(k) ?? 0) + by);
  }

  render(floor) {
    /* push to the physical board / websocket */
  }
}

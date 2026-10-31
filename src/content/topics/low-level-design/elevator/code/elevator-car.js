export class ElevatorCar {
  #stops = new Set();
  #doorOpen = false;

  constructor({ id, minFloor, maxFloor, floor = minFloor }) {
    Object.assign(this, { id, minFloor, maxFloor, floor, direction: 'IDLE' });
  }

  addStop(f) {
    if (f < this.minFloor || f > this.maxFloor) throw new RangeError(`no floor ${f}`);
    this.#stops.add(f);
    if (this.direction === 'IDLE' && f !== this.floor) this.direction = f > this.floor ? 'UP' : 'DOWN';
  }

  get pendingStops() {
    return this.#stops.size;
  }

  tick() {
    if (this.#doorOpen) { this.#doorOpen = false; return; }
    if (this.#stops.delete(this.floor)) { this.#doorOpen = true; return; }
    const next = this.#nextStop();
    if (next === undefined) { this.direction = 'IDLE'; return; }
    this.direction = next > this.floor ? 'UP' : 'DOWN';
    this.floor += this.direction === 'UP' ? 1 : -1;
  }

  #nextStop() {
    const s = [...this.#stops];
    const above = s.filter((f) => f >= this.floor).sort((a, b) => a - b);
    const below = s.filter((f) => f <= this.floor).sort((a, b) => b - a);
    if (this.direction === 'DOWN') return below[0] ?? above[0];
    return above[0] ?? below[0];
  }
}

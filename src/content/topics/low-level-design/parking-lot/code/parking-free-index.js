// uses FITS_IN and ParkingSpot from the model above
// O(log n) allocation: one min-heap of free spots per size, ordered by (floor, id) = "nearest".
const nearer = (a, b) => a.floor - b.floor || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0);

class SpotHeap {
  items = [];
  push(s) {
    const a = this.items;
    a.push(s);
    for (let i = a.length - 1; i > 0; ) {
      const p = (i - 1) >> 1;
      if (nearer(a[i], a[p]) >= 0) break;
      [a[i], a[p]] = [a[p], a[i]];
      i = p;
    }
  }
  pop() {
    const a = this.items;
    if (!a.length) return null;
    const top = a[0];
    const last = a.pop();
    if (a.length) {
      a[0] = last;
      for (let i = 0; ; ) {
        const l = 2 * i + 1, r = l + 1;
        let m = i;
        if (l < a.length && nearer(a[l], a[m]) < 0) m = l;
        if (r < a.length && nearer(a[r], a[m]) < 0) m = r;
        if (m === i) break;
        [a[i], a[m]] = [a[m], a[i]];
        i = m;
      }
    }
    return top;
  }
}

export class FreeSpotIndex {
  #free = { SMALL: new SpotHeap(), MEDIUM: new SpotHeap(), LARGE: new SpotHeap() };
  constructor(spots) {
    spots.forEach((s) => this.#free[s.size].push(s));
  }
  claim(vehicleType) {
    for (const size of FITS_IN[vehicleType]) {
      const s = this.#free[size].pop();
      if (s) return s;
    }
    return null;
  }
  giveBack(spot) {
    this.#free[spot.size].push(spot);
  }
}

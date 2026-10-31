// A uniform grid of ~1 km cells: updating a location is O(1); "nearby" scans the
// surrounding cells.
const CELL_DEG = 0.009; // ≈ 1 km of latitude
const cellKey = (p) => `${Math.floor(p.lat / CELL_DEG)}:${Math.floor(p.lng / CELL_DEG)}`;

export function distanceKm(a, b) {
  const rad = (d) => (d * Math.PI) / 180;
  const h = Math.sin(rad(b.lat - a.lat) / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(rad(b.lng - a.lng) / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(h));
}

export class GridIndex {
  #cells = new Map(); // cell -> Set<driver>
  #cellOf = new Map(); // driverId -> cell

  update(driver, p) {                             // called on every GPS ping
    const key = cellKey(p);
    const old = this.#cellOf.get(driver.id);
    if (old !== undefined && old !== key) this.#cells.get(old)?.delete(driver);
    if (!this.#cells.has(key)) this.#cells.set(key, new Set());
    this.#cells.get(key).add(driver);
    this.#cellOf.set(driver.id, key);
    driver.location = p;
  }

  near(p, radiusKm) {
    const r = Math.ceil(radiusKm), row = Math.floor(p.lat / CELL_DEG), col = Math.floor(p.lng / CELL_DEG);
    const out = [];
    for (let dr = -r; dr <= r; dr++)
      for (let dc = -r; dc <= r; dc++) for (const d of this.#cells.get(`${row + dr}:${col + dc}`) ?? []) out.push(d);
    return out.filter((d) => distanceKm(d.location, p) <= radiusKm);
  }
}

// In-memory index of moving drivers for one city shard: frequent updates, expiry, nearest-k by rings.
const CELL = 0.01; // degrees ≈ 1.1 km of latitude
const cellKey = (lat, lon) => `${Math.floor(lat / CELL)}:${Math.floor(lon / CELL)}`;

export function km(lat1, lon1, lat2, lon2) {
  const rad = (d) => (d * Math.PI) / 180;
  const a = Math.sin(rad(lat2 - lat1) / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(a));
}

export class DriverIndex {
  #drivers = new Map(); // id -> { lat, lon, seenAt, available }
  #cells = new Map(); // "y:x" -> Set<id>

  constructor(staleAfterSeconds = 30) {
    this.staleAfter = staleAfterSeconds;
  }

  update(id, lat, lon, available, now) {
    const old = this.#drivers.get(id);
    if (old && cellKey(old.lat, old.lon) !== cellKey(lat, lon)) this.#cells.get(cellKey(old.lat, old.lon)).delete(id);
    this.#drivers.set(id, { lat, lon, seenAt: now, available });
    const key = cellKey(lat, lon);
    if (!this.#cells.has(key)) this.#cells.set(key, new Set());
    this.#cells.get(key).add(id);
  }

  claim(id) {
    const d = this.#drivers.get(id);
    if (!d || !d.available) return false;
    d.available = false;
    return true;
  }

  evictStale(now) {
    let n = 0;
    for (const [id, d] of this.#drivers)
      if (now - d.seenAt > this.staleAfter) {
        this.#drivers.delete(id);
        this.#cells.get(cellKey(d.lat, d.lon)).delete(id);
        n++;
      }
    return n;
  }

  nearest(lat, lon, k, maxKm, now) {
    const cy = Math.floor(lat / CELL);
    const cx = Math.floor(lon / CELL);
    const cellKm = CELL * 111 * Math.max(Math.cos((lat * Math.PI) / 180), 0.1);
    const maxRing = Math.ceil(maxKm / cellKm) + 1;
    const found = [];
    for (let ring = 0; ring <= maxRing; ring++) {
      for (let dy = -ring; dy <= ring; dy++)
        for (let dx = -ring; dx <= ring; dx++) {
          if (Math.max(Math.abs(dy), Math.abs(dx)) !== ring) continue;
          for (const id of this.#cells.get(`${cy + dy}:${cx + dx}`) ?? []) {
            const d = this.#drivers.get(id);
            if (!d.available || now - d.seenAt > this.staleAfter) continue;
            const dist = km(lat, lon, d.lat, d.lon);
            if (dist <= maxKm) found.push([dist, id]);
          }
        }
      found.sort((a, b) => a[0] - b[0] || (a[1] < b[1] ? -1 : 1));
      if (found.length >= k && found[k - 1][0] <= ring * cellKm) break;
    }
    return found.slice(0, k).map(([, id]) => id);
  }
}

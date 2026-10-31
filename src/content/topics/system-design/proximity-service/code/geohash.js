const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

export function encode(lat, lon, precision) {
  const latR = [-90, 90];
  const lonR = [-180, 180];
  let out = '';
  let bits = 0;
  let ch = 0;
  let even = true;
  while (out.length < precision) {
    const r = even ? lonR : latR;
    const v = even ? lon : lat;
    const mid = (r[0] + r[1]) / 2;
    if (v >= mid) [ch, r[0]] = [(ch << 1) | 1, mid];
    else [ch, r[1]] = [ch << 1, mid];
    even = !even;
    if (++bits === 5) [out, bits, ch] = [out + BASE32[ch], 0, 0];
  }
  return out;
}

export function bounds(gh) {
  const latR = [-90, 90];
  const lonR = [-180, 180];
  let even = true;
  for (const c of gh) {
    const v = BASE32.indexOf(c);
    for (let shift = 4; shift >= 0; shift--) {
      const r = even ? lonR : latR;
      const mid = (r[0] + r[1]) / 2;
      if ((v >> shift) & 1) r[0] = mid;
      else r[1] = mid;
      even = !even;
    }
  }
  return [latR[0], latR[1], lonR[0], lonR[1]];
}

export function neighbours(gh) {
  const [minLat, maxLat, minLon, maxLon] = bounds(gh);
  const [h, w] = [maxLat - minLat, maxLon - minLon];
  const [latC, lonC] = [(minLat + maxLat) / 2, (minLon + maxLon) / 2];
  const cells = new Set();
  for (const dy of [-1, 0, 1])
    for (const dx of [-1, 0, 1]) {
      const lat = latC + dy * h;
      if (lat < -90 || lat > 90) continue;
      cells.add(encode(lat, ((lonC + dx * w + 540) % 360) - 180, gh.length));
    }
  return [...cells].sort();
}

export function km(lat1, lon1, lat2, lon2) {
  const rad = (d) => (d * Math.PI) / 180;
  const a =
    Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(a));
}

// index: Map(cell -> [{ id, lat, lon }]); cells must be at least radiusKm wide.
export function nearby(index, lat, lon, radiusKm, precision) {
  const hits = [];
  for (const cell of neighbours(encode(lat, lon, precision)))
    for (const p of index.get(cell) ?? []) {
      const d = km(lat, lon, p.lat, p.lon);
      if (d <= radiusKm) hits.push([d, p.id]);
    }
  return hits.sort((a, b) => a[0] - b[0]).map(([, id]) => id);
}

import math

BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

def encode(lat: float, lon: float, precision: int) -> str:
    """Interleave longitude/latitude bisection bits, 5 bits per base-32 character."""
    lat_r, lon_r = [-90.0, 90.0], [-180.0, 180.0]
    out, bits, ch, even = [], 0, 0, True
    while len(out) < precision:
        rng, val = (lon_r, lon) if even else (lat_r, lat)
        mid = (rng[0] + rng[1]) / 2
        if val >= mid:
            ch, rng[0] = (ch << 1) | 1, mid
        else:
            ch, rng[1] = ch << 1, mid
        even, bits = not even, bits + 1
        if bits == 5:
            out.append(BASE32[ch])
            bits, ch = 0, 0
    return "".join(out)

def bounds(gh: str) -> tuple[float, float, float, float]:
    """(min_lat, max_lat, min_lon, max_lon) of a cell."""
    lat_r, lon_r, even = [-90.0, 90.0], [-180.0, 180.0], True
    for c in gh:
        v = BASE32.index(c)
        for shift in range(4, -1, -1):
            rng = lon_r if even else lat_r
            mid = (rng[0] + rng[1]) / 2
            if (v >> shift) & 1:
                rng[0] = mid
            else:
                rng[1] = mid
            even = not even
    return lat_r[0], lat_r[1], lon_r[0], lon_r[1]

def neighbours(gh: str) -> list[str]:
    """The cell and its 8 neighbours (a point near an edge has its true neighbours next door)."""
    min_lat, max_lat, min_lon, max_lon = bounds(gh)
    h, w = max_lat - min_lat, max_lon - min_lon
    lat_c, lon_c = (min_lat + max_lat) / 2, (min_lon + max_lon) / 2
    cells = []
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            lat = lat_c + dy * h
            if not -90 <= lat <= 90:
                continue
            lon = (lon_c + dx * w + 540) % 360 - 180              # wrap at the antimeridian
            cells.append(encode(lat, lon, len(gh)))
    return sorted(set(cells))

def km(lat1, lon1, lat2, lon2) -> float:
    p1, p2, dl = math.radians(lat1), math.radians(lat2), math.radians(lon2 - lon1)
    a = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(a))

def nearby(index: dict[str, list[tuple[str, float, float]]], lat: float, lon: float,
           radius_km: float, precision: int) -> list[tuple[float, str]]:
    """index: geohash prefix (length = precision) → places. Cells must be at least radius_km wide."""
    hits = []
    for cell in neighbours(encode(lat, lon, precision)):
        for place_id, plat, plon in index.get(cell, []):
            d = km(lat, lon, plat, plon)
            if d <= radius_km:
                hits.append((d, place_id))
    return sorted(hits)

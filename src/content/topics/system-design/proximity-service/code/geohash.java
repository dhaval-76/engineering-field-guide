final class Geohash {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /** Interleave longitude/latitude bisection bits, 5 bits per base-32 character. */
    static String encode(double lat, double lon, int precision) {
        double[] latR = {-90, 90}, lonR = {-180, 180};
        StringBuilder out = new StringBuilder();
        int bits = 0, ch = 0;
        boolean even = true;
        while (out.length() < precision) {
            double[] r = even ? lonR : latR;
            double v = even ? lon : lat, mid = (r[0] + r[1]) / 2;
            if (v >= mid) { ch = (ch << 1) | 1; r[0] = mid; } else { ch <<= 1; r[1] = mid; }
            even = !even;
            if (++bits == 5) { out.append(BASE32.charAt(ch)); bits = 0; ch = 0; }
        }
        return out.toString();
    }

    /** {minLat, maxLat, minLon, maxLon} of a cell. */
    static double[] bounds(String gh) {
        double[] latR = {-90, 90}, lonR = {-180, 180};
        boolean even = true;
        for (char c : gh.toCharArray()) {
            int v = BASE32.indexOf(c);
            for (int shift = 4; shift >= 0; shift--) {
                double[] r = even ? lonR : latR;
                double mid = (r[0] + r[1]) / 2;
                if (((v >> shift) & 1) == 1) r[0] = mid; else r[1] = mid;
                even = !even;
            }
        }
        return new double[] {latR[0], latR[1], lonR[0], lonR[1]};
    }

    /** The cell and its 8 neighbours. */
    static SortedSet<String> neighbours(String gh) {
        double[] b = bounds(gh);
        double h = b[1] - b[0], w = b[3] - b[2], latC = (b[0] + b[1]) / 2, lonC = (b[2] + b[3]) / 2;
        SortedSet<String> cells = new TreeSet<>();
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                double lat = latC + dy * h;
                if (lat < -90 || lat > 90) continue;
                // wrap at the antimeridian
                double lon = ((lonC + dx * w + 540) % 360) - 180;
                cells.add(encode(lat, lon, gh.length()));
            }
        return cells;
    }

    static double km(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(dl / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(a));
    }

    record Place(String id, double lat, double lon) {}

    /**
     * index: geohash cell → places. Cells must be at least radiusKm wide for 9 cells to
     * cover the circle.
     */
    static List<String> nearby(Map<String, List<Place>> index, double lat, double lon, double radiusKm, int precision) {
        List<Map.Entry<Double, String>> hits = new ArrayList<>();
        for (String cell : neighbours(encode(lat, lon, precision)))
            for (Place p : index.getOrDefault(cell, List.of())) {
                double d = km(lat, lon, p.lat(), p.lon());
                if (d <= radiusKm) hits.add(Map.entry(d, p.id()));
            }
        hits.sort(Map.Entry.comparingByKey());
        return hits.stream().map(Map.Entry::getValue).toList();
    }
}

record Estimate(double avgQps, double peakQps, double storageTbPerYear, double egressMbPerSec) {

    static Estimate of(long requestsPerDay, double peakFactor, long newItemsPerDay,
                       int bytesPerItem, int replicas, int bytesPerResponse) {
        double avg = requestsPerDay / 86_400.0;
        double peak = avg * peakFactor;
        double storage = (double) newItemsPerDay * bytesPerItem * 365 * replicas / 1e12;
        double egress = peak * bytesPerResponse / 1e6;
        return new Estimate(avg, peak, storage, egress);
    }

    public static void main(String[] args) {
        // URL shortener: 10 B redirects/day, 100 M new links/day
        var e = of(10_000_000_000L, 3, 100_000_000L, 500, 3, 500);
        System.out.printf("avg %.0f/s, peak %.0f/s, %.1f TB/yr, %.0f MB/s%n",
                e.avgQps(), e.peakQps(), e.storageTbPerYear(), e.egressMbPerSec());
        // avg 115741/s, peak 347222/s, 54.8 TB/yr, 174 MB/s
    }
}

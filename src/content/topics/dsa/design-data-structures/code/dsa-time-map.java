class TimeMap {
    private record Version(int timestamp, String value) {}
    // per key, timestamps increase
    private final Map<String, List<Version>> history = new HashMap<>();

    void set(String key, String value, int timestamp) {
        history.computeIfAbsent(key, k -> new ArrayList<>()).add(new Version(timestamp, value));
    }

    /** Value with the largest timestamp ≤ t, or "". O(log n) per get. */
    String get(String key, int t) {
        List<Version> versions = history.getOrDefault(key, List.of());
        // first index with timestamp > t
        int lo = 0, hi = versions.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (versions.get(mid).timestamp() > t) hi = mid; else lo = mid + 1;
        }
        return lo == 0 ? "" : versions.get(lo - 1).value();
    }
}
// Out-of-order timestamps? Use a TreeMap<Integer, String> per key and floorEntry(t).

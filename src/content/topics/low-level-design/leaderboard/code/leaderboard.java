/**
 * In-memory leaderboard: higher score first; ties go to whoever reached the score first.
 */
class Leaderboard {
    private record Entry(String player, long score, long reachedAt) {}

    private static final Comparator<Entry> ORDER = Comparator
            .comparingLong(Entry::score).reversed()
            .thenComparingLong(Entry::reachedAt)
            .thenComparing(Entry::player);

    private final Map<String, Entry> byPlayer = new HashMap<>();
    // red-black tree: O(log n) updates
    private final TreeSet<Entry> ranking = new TreeSet<>(ORDER);
    // logical time for tie-breaks
    private long clock = 0;

    synchronized long addScore(String player, long delta) {
        Entry old = byPlayer.get(player);
        if (old != null) ranking.remove(old);
        Entry updated = new Entry(player, (old == null ? 0 : old.score()) + delta, ++clock);
        byPlayer.put(player, updated);
        ranking.add(updated);
        return updated.score();
    }

    synchronized List<Map.Entry<String, Long>> top(int k) {             // O(k + log n)
        List<Map.Entry<String, Long>> out = new ArrayList<>();
        for (Entry e : ranking) {
            if (out.size() == k) break;
            out.add(Map.entry(e.player(), e.score()));
        }
        return out;
    }

    /**
     * 1-based rank, or -1. headSet().size() walks the tree: O(n). Redis ZREVRANK does
     * this in O(log n).
     */
    synchronized int rank(String player) {
        Entry e = byPlayer.get(player);
        return e == null ? -1 : ranking.headSet(e).size() + 1;
    }

    synchronized void remove(String player) {
        Entry e = byPlayer.remove(player);
        if (e != null) ranking.remove(e);
    }
}

/**
 * Ranked autocomplete: every node caches the top K completions under it, so a query is
 * O(prefix length) — walk down and read the cached list. Updates touch only the word's path.
 */
final class RankedAutocomplete {
    private static final int K = 5;
    private record Scored(String word, long score) {}

    private static final class Node {
        final Map<Character, Node> next = new HashMap<>();
        final List<Scored> top = new ArrayList<>();          // at most K, sorted by score desc, then word
    }

    private final Node root = new Node();
    private final Map<String, Long> scores = new HashMap<>();

    /** Add `delta` to a phrase's score (e.g. +1 per search) and refresh the caches on its path. */
    synchronized void record(String phrase, long delta) {
        String p = phrase.trim().toLowerCase(Locale.ROOT);
        long score = scores.merge(p, delta, Long::sum);
        Scored s = new Scored(p, score);
        Node n = root;
        update(n, s);
        for (char c : p.toCharArray()) {
            n = n.next.computeIfAbsent(c, k -> new Node());
            update(n, s);
        }
    }

    private static void update(Node n, Scored s) {
        n.top.removeIf(x -> x.word().equals(s.word()));      // no duplicate entries for the same phrase
        n.top.add(s);
        n.top.sort(Comparator.comparingLong(Scored::score).reversed().thenComparing(Scored::word));
        if (n.top.size() > K) n.top.remove(n.top.size() - 1);
    }

    synchronized List<String> suggest(String prefix) {
        Node n = root;
        for (char c : prefix.toLowerCase(Locale.ROOT).toCharArray()) {
            n = n.next.get(c);
            if (n == null) return List.of();
        }
        return n.top.stream().map(Scored::word).toList();
    }
}
// Note: if a phrase's score can DROP (decay, deletion), a node may need a refill from its subtree,
// because the K-th best was evicted earlier. Rebuilding the caches periodically (offline) avoids that.

final class Trie {
    private static final class Node {
        // or Node[26] for lowercase a–z
        final Map<Character, Node> next = new HashMap<>();
        boolean word;
    }
    private final Node root = new Node();

    void insert(String w) {
        Node n = root;
        for (char c : w.toCharArray()) n = n.next.computeIfAbsent(c, k -> new Node());
        n.word = true;
    }

    boolean contains(String w)       { Node n = walk(w); return n != null && n.word; }
    boolean hasPrefix(String prefix) { return walk(prefix) != null; }

    /** All words with this prefix, in lexicographic order, up to `limit`. */
    List<String> complete(String prefix, int limit) {
        Node start = walk(prefix);
        List<String> out = new ArrayList<>();
        if (start != null) collect(start, new StringBuilder(prefix), out, limit);
        return out;
    }

    private void collect(Node n, StringBuilder sb, List<String> out, int limit) {
        if (out.size() >= limit) return;
        if (n.word) out.add(sb.toString());
        // sorted children → sorted output
        for (var e : new TreeMap<>(n.next).entrySet()) {
            sb.append(e.getKey());
            collect(e.getValue(), sb, out, limit);
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    private Node walk(String s) {
        Node n = root;
        for (char c : s.toCharArray()) { n = n.next.get(c); if (n == null) return null; }
        return n;
    }

    /** Remove a word and prune nodes that no longer lead anywhere. */
    boolean delete(String w) { return delete(root, w, 0); }
    private boolean delete(Node n, String w, int i) {
        if (i == w.length()) { if (!n.word) return false; n.word = false; return true; }
        Node child = n.next.get(w.charAt(i));
        if (child == null || !delete(child, w, i + 1)) return false;
        if (!child.word && child.next.isEmpty()) n.next.remove(w.charAt(i));
        return true;
    }
}

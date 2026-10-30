class Trie {
    private static final class Node {
        final Map<Character, Node> children = new HashMap<>();   // or Node[26] for lowercase letters
        boolean isWord;
    }
    private final Node root = new Node();

    void insert(String word) {
        Node n = root;
        for (char c : word.toCharArray()) n = n.children.computeIfAbsent(c, k -> new Node());
        n.isWord = true;
    }

    boolean search(String word) {
        Node n = walk(word);
        return n != null && n.isWord;
    }

    boolean startsWith(String prefix) { return walk(prefix) != null; }

    private Node walk(String s) {
        Node n = root;
        for (char c : s.toCharArray()) {
            n = n.children.get(c);
            if (n == null) return null;
        }
        return n;
    }
}
// Each operation is O(L) for a string of length L, regardless of how many words are stored.

/** Digits-only trie with the contact stored at the end node; prefix search returns contacts. */
final class PhoneDirectory {
    private static final class Node {
        final Node[] next = new Node[10];
        final List<Contact> contacts = new ArrayList<>();
    }

    record Contact(String name, String number) {}

    private final Node root = new Node();

    void add(Contact c) {
        Node n = root;
        for (char d : c.number().toCharArray()) {
            int i = d - '0';
            if (i < 0 || i > 9) throw new IllegalArgumentException("digits only: " + c.number());
            n = n.next[i] == null ? (n.next[i] = new Node()) : n.next[i];
        }
        n.contacts.add(c);
    }

    List<Contact> byPrefix(String digits, int limit) {
        Node n = root;
        for (char d : digits.toCharArray()) {
            n = n.next[d - '0'];
            if (n == null) return List.of();
        }
        List<Contact> out = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>(List.of(n));
        while (!stack.isEmpty() && out.size() < limit) {
            Node cur = stack.pop();
            out.addAll(cur.contacts);
            // 0 first
            for (int i = 9; i >= 0; i--) if (cur.next[i] != null) stack.push(cur.next[i]);
        }
        return out.subList(0, Math.min(limit, out.size()));
    }
}

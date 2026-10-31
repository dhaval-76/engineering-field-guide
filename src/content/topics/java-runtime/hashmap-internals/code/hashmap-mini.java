/**
 * A teaching version of java.util.HashMap: array of buckets, chaining, power-of-two
 * resize.
 */
public final class MiniHashMap<K, V> {
    private static final class Node<K, V> {
        final int hash; final K key; V value; Node<K, V> next;
        Node(int hash, K key, V value, Node<K, V> next) { this.hash = hash; this.key = key; this.value = value; this.next = next; }
    }

    private Node<K, V>[] table = newTable(16);
    private int size;
    private static final float LOAD_FACTOR = 0.75f;

    /** Spread high bits into low bits — the index only uses the low bits. */
    private static int hash(Object key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    public V get(K key) {
        int h = hash(key);
        for (Node<K, V> n = table[h & (table.length - 1)]; n != null; n = n.next)
            // compare hash first: cheap filter
            if (n.hash == h && Objects.equals(n.key, key)) return n.value;
        return null;
    }

    public V put(K key, V value) {
        int h = hash(key), i = h & (table.length - 1);
        for (Node<K, V> n = table[i]; n != null; n = n.next) {
            if (n.hash == h && Objects.equals(n.key, key)) { V old = n.value; n.value = value; return old; }
        }
        table[i] = new Node<>(h, key, value, table[i]);
        if (++size > table.length * LOAD_FACTOR) resize();
        return null;
    }

    /**
     * Double the table. Each entry either stays at index i or moves to i + oldCapacity.
     */
    private void resize() {
        Node<K, V>[] old = table;
        table = newTable(old.length * 2);
        for (Node<K, V> head : old) {
            for (Node<K, V> n = head; n != null; ) {
                Node<K, V> next = n.next;
                // the new bit of the index decides: same or +old.length
                int i = n.hash & (table.length - 1);
                n.next = table[i];
                table[i] = n;
                n = next;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Node<K, V>[] newTable(int n) { return (Node<K, V>[]) new Node[n]; }
}

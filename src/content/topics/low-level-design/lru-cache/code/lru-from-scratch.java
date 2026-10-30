import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {

    private static final class Node<K, V> {
        K key;          // kept so eviction can also remove the map entry
        V value;
        Node<K, V> prev, next;

        Node(K key, V value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> index = new HashMap<>();
    // Sentinels: head.next is the most recently used, tail.prev the least.
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    /** Returns the value or null, and marks the key as most recently used. */
    public V get(K key) {
        Node<K, V> node = index.get(key);
        if (node == null) return null;
        unlink(node);
        pushFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = index.get(key);
        if (node != null) {              // update in place, refresh recency
            node.value = value;
            unlink(node);
            pushFront(node);
            return;
        }
        if (index.size() == capacity) {  // make room first
            Node<K, V> lru = tail.prev;
            unlink(lru);
            index.remove(lru.key);
        }
        node = new Node<>(key, value);
        index.put(key, node);
        pushFront(node);
    }

    private void pushFront(Node<K, V> n) {
        n.prev = head;
        n.next = head.next;
        head.next.prev = n;
        head.next = n;
    }

    private void unlink(Node<K, V> n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }
}

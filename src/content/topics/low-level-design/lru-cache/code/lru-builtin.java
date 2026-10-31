import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LinkedHashMap already keeps a doubly linked list; accessOrder=true makes it
 * LRU-ordered.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(16, 0.75f, true);   // true = order by access, not insertion
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity; // called after every put; true evicts the LRU entry
    }
}

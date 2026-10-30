final class MutableKey {
    String id;
    MutableKey(String id) { this.id = id; }
    @Override public boolean equals(Object o) { return o instanceof MutableKey k && k.id.equals(id); }
    @Override public int hashCode() { return id.hashCode(); }
}

Map<MutableKey, String> map = new HashMap<>();
MutableKey key = new MutableKey("A");
map.put(key, "value");

key.id = "B";                         // hashCode changes, but the entry stays in A's bucket
map.get(key);                         // null — looks in B's bucket
map.get(new MutableKey("A"));         // null — right bucket, but equals() now fails
map.size();                           // 1 — the entry is stranded: unreachable but still referenced

// Fix: use immutable keys (String, Long, enums, records with immutable fields).
record OrderKey(String tenant, long orderId) {}

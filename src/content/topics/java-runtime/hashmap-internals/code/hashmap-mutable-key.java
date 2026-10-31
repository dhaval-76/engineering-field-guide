final class MutableKey {
    String id;
    MutableKey(String id) { this.id = id; }
    @Override public boolean equals(Object o) { return o instanceof MutableKey k && k.id.equals(id); }
    @Override public int hashCode() { return id.hashCode(); }
}

Map<MutableKey, String> map = new HashMap<>();
MutableKey key = new MutableKey("A");
map.put(key, "value");

// hashCode changes, but the entry stays in A's bucket
key.id = "B";
map.get(key);                         // null — looks in B's bucket
map.get(new MutableKey("A"));         // null — right bucket, but equals() now fails
// 1 — the entry is stranded: unreachable but still referenced
map.size();

// Fix: use immutable keys (String, Long, enums, records with immutable fields).
record OrderKey(String tenant, long orderId) {}

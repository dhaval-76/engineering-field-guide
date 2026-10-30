final class HashRing {
    private final int virtualNodesPerNode;
    private final NavigableMap<Long, String> ring = new TreeMap<>();   // token → physical node

    HashRing(int virtualNodesPerNode) { this.virtualNodesPerNode = virtualNodesPerNode; }

    synchronized void addNode(String node) {
        for (int i = 0; i < virtualNodesPerNode; i++) ring.put(hash(node + "#" + i), node);
    }

    synchronized void removeNode(String node) {
        ring.values().removeIf(node::equals);       // only that node's arcs change owner
    }

    /** The first n DISTINCT physical nodes clockwise from the key's position. */
    synchronized List<String> replicasFor(String key, int n) {
        if (ring.isEmpty()) throw new IllegalStateException("empty ring");
        LinkedHashSet<String> replicas = new LinkedHashSet<>();
        Iterator<String> clockwise = Stream.concat(
                ring.tailMap(hash(key), true).values().stream(),
                ring.headMap(hash(key), false).values().stream()).iterator();
        while (clockwise.hasNext() && replicas.size() < n) replicas.add(clockwise.next());
        return List.copyOf(replicas);
    }

    private static long hash(String s) {
        // Stable 64-bit hash (first 8 bytes of MD5); not for security, just even spread.
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(d).getLong();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
// new HashRing(128) → addNode("A"), addNode("B"), addNode("C") → replicasFor("cart:42", 3)

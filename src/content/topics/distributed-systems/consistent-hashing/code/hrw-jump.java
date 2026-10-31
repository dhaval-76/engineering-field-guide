/**
 * Rendezvous (highest-random-weight) hashing: every key scores every node; the highest
 * score wins.
 */
static String rendezvous(String key, List<String> nodes) {
    String best = null;
    long bestScore = 0;
    for (String node : nodes) {
        long score = hash64(node + "|" + key);
        if (best == null || Long.compareUnsigned(score, bestScore) > 0) { best = node; bestScore = score; }
    }
    // removing a node only moves the keys it won
    return best;
}

/**
 * Jump consistent hash (Lamping & Veach): bucket in [0, buckets) with no ring and no
 * memory.
 */
static int jumpHash(long key, int buckets) {
    long b = -1, j = 0;
    while (j < buckets) {
        b = j;
        key = key * 2862933555777941757L + 1;
        j = (long) ((b + 1) * ((double) (1L << 31) / (double) ((key >>> 33) + 1)));
    }
    // buckets can only be added/removed at the end
    return (int) b;
}

static long hash64(String s) {                       // first 8 bytes of MD5, big-endian
    try {
        byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(d).getLong();
    } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
}

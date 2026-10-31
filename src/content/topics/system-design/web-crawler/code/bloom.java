/** "Have we seen this URL?" in ~10 bits per URL. No false negatives; a tunable rate of false positives. */
class BloomFilter {
    final long m; final int k;
    private final long[] bits;

    BloomFilter(long expected, double fpRate) {
        m = (long) Math.ceil(-expected * Math.log(fpRate) / (Math.log(2) * Math.log(2)));
        k = Math.max(1, (int) Math.round((double) m / expected * Math.log(2)));
        bits = new long[(int) ((m + 63) / 64)];
    }

    private long[] positions(String item) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(item.getBytes(StandardCharsets.UTF_8));
        long h1 = ByteBuffer.wrap(d, 0, 8).getLong(), h2 = ByteBuffer.wrap(d, 8, 8).getLong() | 1;
        // reduce first: no overflow
        long a = Long.remainderUnsigned(h1, m), b = Long.remainderUnsigned(h2, m);
        long[] p = new long[k];
        // double hashing
        for (int i = 0; i < k; i++) p[i] = (a + i * b) % m;
        return p;
    }

    void add(String item) throws Exception {
        for (long p : positions(item)) bits[(int) (p >>> 6)] |= 1L << (p & 63);
    }

    boolean mightContain(String item) throws Exception {
        for (long p : positions(item)) if ((bits[(int) (p >>> 6)] & (1L << (p & 63))) == 0) return false;
        return true;
    }
}

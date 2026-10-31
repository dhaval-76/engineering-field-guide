/** Content-defined chunking with a gear rolling hash; boundaries follow content, not offsets. */
final class Chunker {
    private static final long[] GEAR = new long[256];
    static {                                                   // xorshift64*: same table on every client
        long x = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < 256; i++) {
            x ^= x >>> 12; x ^= x << 25; x ^= x >>> 27;
            GEAR[i] = x * 0x2545F4914F6CDD1DL;
        }
    }

    static List<byte[]> chunk(byte[] data, int minSize, int avgBits, int maxSize) {
        long mask = ((1L << avgBits) - 1) << (64 - avgBits);   // test the high bits
        List<byte[]> chunks = new ArrayList<>();
        int start = 0;
        long h = 0;
        for (int i = 0; i < data.length; i++) {
            h = (h << 1) + GEAR[data[i] & 0xFF];
            int size = i - start + 1;
            if ((size >= minSize && (h & mask) == 0) || size >= maxSize) {
                chunks.add(Arrays.copyOfRange(data, start, i + 1));
                start = i + 1;
                h = 0;
            }
        }
        if (start < data.length) chunks.add(Arrays.copyOfRange(data, start, data.length));
        return chunks;
    }

    /** Chunks are stored once, named by SHA-256: identical content dedupes across files and users. */
    static List<String> chunkIds(byte[] data) throws Exception {
        List<String> ids = new ArrayList<>();
        for (byte[] c : chunk(data, 2048, 13, 65536))
            ids.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c)));
        return ids;
    }
}

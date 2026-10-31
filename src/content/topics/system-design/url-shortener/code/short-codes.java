final class ShortCodes {
    private static final char[] ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    // 62^7: every 7-char code
    private static final BigInteger SPACE = BigInteger.valueOf(3_521_614_606_208L);
    // Coprime with 62^7 = 2^7 · 31^7 (odd, not a multiple of 31) → multiplication mod
    // 62^7 is a bijection
    private static final BigInteger MULTIPLIER = BigInteger.valueOf(1_580_030_173L);

    /** Fixed-width base62 so every code has 7 characters. */
    static String encode(long n) {
        char[] out = new char[7];
        for (int i = 6; i >= 0; i--) { out[i] = ALPHABET[(int) (n % 62)]; n /= 62; }
        return new String(out);
    }

    static long decode(String code) {
        long n = 0;
        for (char c : code.toCharArray()) n = n * 62 + indexOf(c);
        return n;
    }

    /**
     * Spread consecutive ids across the code space so codes aren't sequential (not
     * encryption).
     */
    static String scrambled(long id) {
        BigInteger n = BigInteger.valueOf(id);
        if (n.signum() < 0 || n.compareTo(SPACE) >= 0) throw new IllegalArgumentException("id out of range");
        // 128-bit math: no overflow
        return encode(n.multiply(MULTIPLIER).mod(SPACE).longValueExact());
    }

    private static int indexOf(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'z') return c - 'a' + 10;
        if (c >= 'A' && c <= 'Z') return c - 'A' + 36;
        throw new IllegalArgumentException("not base62: " + c);
    }
}

/**
 * Each instance reserves 10,000 ids at a time, so creating a link needs no shared call.
 */
final class IdRange {
    private final JdbcTemplate jdbc;
    private long next, end;                                        // [next, end)

    synchronized long nextId() {
        if (next == end) {
            Long start = jdbc.queryForObject(
                "UPDATE id_blocks SET next_value = next_value + 10000 WHERE name = 'links' RETURNING next_value - 10000",
                Long.class);
            next = start; end = start + 10_000;
        }
        return next++;
    }
}

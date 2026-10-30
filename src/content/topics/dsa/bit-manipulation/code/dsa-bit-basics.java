/** Every element appears twice except one: pairs cancel under XOR. */
int singleNumber(int[] nums) {
    int x = 0;
    for (int n : nums) x ^= n;
    return x;
}

/** nums contains n distinct values from 0..n with one missing. */
int missingNumber(int[] nums) {
    int x = nums.length;                          // include n itself
    for (int i = 0; i < nums.length; i++) x ^= i ^ nums[i];
    return x;
}

/** Number of set bits: each iteration removes the lowest one. */
int popcount(int x) {
    int count = 0;
    while (x != 0) { x &= x - 1; count++; }       // works for negatives too (32 iterations max)
    return count;                                 // or Integer.bitCount(x)
}

/** bits[i] = popcount(i) for 0..n, using bits[i >> 1] + lowest bit. O(n). */
int[] countBits(int n) {
    int[] bits = new int[n + 1];
    for (int i = 1; i <= n; i++) bits[i] = bits[i >> 1] + (i & 1);
    return bits;
}

boolean isPowerOfTwo(long x) { return x > 0 && (x & (x - 1)) == 0; }

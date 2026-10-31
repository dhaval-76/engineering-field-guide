/** All subsets via masks 0 .. 2^n − 1: bit i set ⇔ nums[i] is in the subset. */
List<List<Integer>> subsetsByMask(int[] nums) {
    int n = nums.length;
    List<List<Integer>> out = new ArrayList<>();
    for (int mask = 0; mask < (1 << n); mask++) {
        List<Integer> subset = new ArrayList<>();
        for (int i = 0; i < n; i++)
            if ((mask & (1 << i)) != 0) subset.add(nums[i]);
        out.add(subset);
    }
    return out;
}

/**
 * Every non-empty submask of `mask` (useful in bitmask DP); total over all masks is
 * O(3^n).
 */
List<Integer> submasks(int mask) {
    List<Integer> out = new ArrayList<>();
    for (int sub = mask; sub > 0; sub = (sub - 1) & mask) out.add(sub);
    return out;
}

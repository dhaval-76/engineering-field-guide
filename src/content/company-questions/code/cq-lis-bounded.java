int lengthOfLIS(int[] nums, int k) {
    int size = Arrays.stream(nums).max().getAsInt() + 1;
    int[] tree = new int[2 * size]; // segment tree: best length ending at each value
    int best = 0;
    for (int value : nums) {
        int length = query(tree, size, Math.max(0, value - k), value) + 1;
        best = Math.max(best, length);
        update(tree, size, value, length);
    }
    return best;
}

/** Maximum over values in [lo, hi). */
int query(int[] tree, int size, int lo, int hi) {
    int max = 0;
    for (lo += size, hi += size; lo < hi; lo >>= 1, hi >>= 1) {
        if ((lo & 1) == 1) max = Math.max(max, tree[lo++]);
        if ((hi & 1) == 1) max = Math.max(max, tree[--hi]);
    }
    return max;
}

void update(int[] tree, int size, int index, int value) {
    for (int i = index + size; i >= 1 && tree[i] < value; i >>= 1) tree[i] = value;
}

// Converging: pair in a SORTED array with a given sum (returns 1-based indices, or empty)
int[] pairWithSum(int[] sorted, int target) {
    int l = 0, r = sorted.length - 1;
    while (l < r) {
        int sum = sorted[l] + sorted[r];
        if (sum == target) return new int[]{l + 1, r + 1};
        if (sum < target) l++;        // need bigger: drop the smallest
        else r--;                     // need smaller: drop the largest
    }
    return new int[0];
}

// Read/write: keep elements that satisfy a condition, in place; returns the new length
int keepIf(int[] nums, IntPredicate keep) {
    int write = 0;
    for (int read = 0; read < nums.length; read++)
        if (keep.test(nums[read])) nums[write++] = nums[read];
    return write;
}

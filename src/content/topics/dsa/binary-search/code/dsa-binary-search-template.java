/** First index i in [0, n) with nums[i] >= target, or n if none (lower bound). */
int lowerBound(int[] nums, int target) {
    int lo = 0, hi = nums.length;               // answer is in [lo, hi]
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;            // no overflow
        if (nums[mid] >= target) hi = mid;       // predicate true: answer at mid or left
        else lo = mid + 1;                       // false: answer strictly right
    }
    return lo;
}

/** Index of target, or -1. */
int search(int[] nums, int target) {
    int i = lowerBound(nums, target);
    return i < nums.length && nums[i] == target ? i : -1;
}

/** Last index with nums[i] == target, or -1. */
int lastIndex(int[] nums, int target) {
    // first index > target, minus one (careful near Integer.MAX_VALUE)
    int i = lowerBound(nums, target + 1) - 1;
    return i >= 0 && nums[i] == target ? i : -1;
}

/** The generic form: smallest x in [lo, hi] with ok(x) true, assuming ok is monotone and ok(hi) is true. */
long firstTrue(long lo, long hi, LongPredicate ok) {
    while (lo < hi) {
        long mid = lo + (hi - lo) / 2;
        if (ok.test(mid)) hi = mid; else lo = mid + 1;
    }
    return lo;
}

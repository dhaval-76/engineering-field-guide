// Fixed size k: maximum sum of any k consecutive elements
long maxSumWindow(int[] nums, int k) {
    long sum = 0, best = Long.MIN_VALUE;
    for (int right = 0; right < nums.length; right++) {
        sum += nums[right];                               // element enters
        if (right >= k) sum -= nums[right - k];           // element leaves
        if (right >= k - 1) best = Math.max(best, sum);
    }
    return best;
}

// Variable: longest window whose sum ≤ limit (non-negative numbers → monotone condition)
int longestWithSumAtMost(int[] nums, long limit) {
    long sum = 0;
    int left = 0, best = 0;
    for (int right = 0; right < nums.length; right++) {
        sum += nums[right];                               // 1. expand
        while (sum > limit) sum -= nums[left++];          // 2. shrink while invalid
        best = Math.max(best, right - left + 1);          // 3. window is valid: record
    }
    return best;
}

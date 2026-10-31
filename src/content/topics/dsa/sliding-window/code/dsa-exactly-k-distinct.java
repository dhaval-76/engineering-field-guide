// Number of subarrays with exactly k distinct values = atMost(k) − atMost(k − 1).
int subarraysWithKDistinct(int[] nums, int k) {
    return atMost(nums, k) - atMost(nums, k - 1);
}

// Subarrays with at most k distinct values: a normal variable window.
int atMost(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    int left = 0, total = 0;
    for (int right = 0; right < nums.length; right++) {
        count.merge(nums[right], 1, Integer::sum);
        while (count.size() > k) {                     // too many distinct: shrink
            if (count.merge(nums[left], -1, Integer::sum) == 0) count.remove(nums[left]);
            left++;
        }
        total += right - left + 1;   // every subarray ending at right and starting ≥ left
    }
    return total;
}
// [1,2,1,2,3], k = 2 → 7: [1,2] [2,1] [1,2] [2,3] [1,2,1] [2,1,2] [1,2,1,2]. O(n).

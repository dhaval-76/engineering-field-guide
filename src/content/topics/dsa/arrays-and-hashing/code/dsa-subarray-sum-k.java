int subarraySum(int[] nums, int k) {
    Map<Long, Integer> prefixCount = new HashMap<>();
    // the empty prefix: subarrays starting at 0
    prefixCount.put(0L, 1);
    long sum = 0;
    int count = 0;
    for (int x : nums) {
        sum += x;
        // earlier prefixes p with sum - p = k
        count += prefixCount.getOrDefault(sum - k, 0);
        prefixCount.merge(sum, 1, Integer::sum);
    }
    return count;
}
// O(n) time, O(n) space; works with negative numbers (a sliding window would not)

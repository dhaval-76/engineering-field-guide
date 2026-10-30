int subarraySum(int[] nums, int k) {
    Map<Long, Integer> prefixCount = new HashMap<>();
    prefixCount.put(0L, 1);                                // the empty prefix: subarrays starting at 0
    long sum = 0;
    int count = 0;
    for (int x : nums) {
        sum += x;
        count += prefixCount.getOrDefault(sum - k, 0);     // earlier prefixes p with sum - p = k
        prefixCount.merge(sum, 1, Integer::sum);
    }
    return count;
}
// O(n) time, O(n) space; works with negative numbers (a sliding window would not)

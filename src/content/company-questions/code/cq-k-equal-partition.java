boolean canPartitionKSubsets(int[] nums, int k) {
    int total = Arrays.stream(nums).sum();
    if (total % k != 0) return false;
    Arrays.sort(nums); // the largest first fails fastest
    return place(nums, nums.length - 1, new int[k], total / k);
}

boolean place(int[] nums, int i, int[] buckets, int target) {
    if (i < 0) return true;
    Set<Integer> triedLoads = new HashSet<>(); // equal loads give identical subtrees
    for (int b = 0; b < buckets.length; b++) {
        if (buckets[b] + nums[i] > target || !triedLoads.add(buckets[b])) continue;
        buckets[b] += nums[i];
        if (place(nums, i - 1, buckets, target)) return true;
        buckets[b] -= nums[i];
    }
    return false;
}

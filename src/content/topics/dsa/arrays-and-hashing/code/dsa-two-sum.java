int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> seen = new HashMap<>();          // value → index
    for (int i = 0; i < nums.length; i++) {
        Integer j = seen.get(target - nums[i]);            // partner seen earlier?
        if (j != null) return new int[]{j, i};
        seen.put(nums[i], i);                              // add AFTER checking: no self-pairing
    }
    return new int[0];
}
// O(n) time, O(n) space

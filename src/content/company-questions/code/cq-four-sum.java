List<List<Integer>> fourSum(int[] nums, int target) {
    Arrays.sort(nums);
    List<List<Integer>> out = new ArrayList<>();
    int n = nums.length;
    for (int a = 0; a < n - 3; a++) {
        if (a > 0 && nums[a] == nums[a - 1]) continue;
        for (int b = a + 1; b < n - 2; b++) {
            if (b > a + 1 && nums[b] == nums[b - 1]) continue;
            int lo = b + 1, hi = n - 1;
            while (lo < hi) {
                long sum = (long) nums[a] + nums[b] + nums[lo] + nums[hi]; // can overflow int
                if (sum < target) lo++;
                else if (sum > target) hi--;
                else {
                    out.add(List.of(nums[a], nums[b], nums[lo], nums[hi]));
                    while (lo < hi && nums[lo] == nums[lo + 1]) lo++;
                    while (lo < hi && nums[hi] == nums[hi - 1]) hi--;
                    lo++;
                    hi--;
                }
            }
        }
    }
    return out;
}

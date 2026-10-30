List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> out = new ArrayList<>();
    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;       // same first value → same triplets
        if (nums[i] > 0) break;                              // sorted: no way to reach 0
        int l = i + 1, r = nums.length - 1;
        while (l < r) {
            int sum = nums[i] + nums[l] + nums[r];
            if (sum < 0) l++;
            else if (sum > 0) r--;
            else {
                out.add(List.of(nums[i], nums[l], nums[r]));
                while (l < r && nums[l] == nums[l + 1]) l++; // skip duplicate second values
                while (l < r && nums[r] == nums[r - 1]) r--;
                l++; r--;
            }
        }
    }
    return out;
}
// O(n²) time, O(1) extra space besides the output (sorting in place)

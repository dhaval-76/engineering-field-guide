int maxSubArray(int[] nums) {
    int best = nums[0], endingHere = nums[0];
    for (int i = 1; i < nums.length; i++) {
        // extend the previous run, or start again here if the run so far only hurts
        endingHere = Math.max(nums[i], endingHere + nums[i]);
        best = Math.max(best, endingHere);
    }
    return best;
}

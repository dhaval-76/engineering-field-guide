int findMin(int[] nums) {
    int lo = 0, hi = nums.length - 1;
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] > nums[hi]) lo = mid + 1;   // the drop is to the right of mid
        else hi = mid;                            // mid is in the lower part: min at mid or left
    }
    return nums[lo];
}
// [3,4,5,1,2] → 1, [4,5,6,7,0,1,2] → 0, [11,13,15,17] → 11

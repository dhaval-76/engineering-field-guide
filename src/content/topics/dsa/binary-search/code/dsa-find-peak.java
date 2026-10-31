// Any index whose value is greater than its neighbours (nums[-1] = nums[n] = −∞).
// The array is NOT sorted; binary search works because the predicate
// "nums[i] > nums[i + 1]" (we are on a downhill slope) is true at some point and,
// following the slope, a peak is always reachable on that side.
int findPeakElement(int[] nums) {
    int lo = 0, hi = nums.length - 1;
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        // downhill: a peak at mid or to the left
        if (nums[mid] > nums[mid + 1]) hi = mid;
        else lo = mid + 1;                         // uphill: a peak strictly to the right
    }
    return lo;
}
// [1,2,1,3,5,6,4] → 5 (value 6) or 1 (value 2); adjacent values must differ. O(log n).

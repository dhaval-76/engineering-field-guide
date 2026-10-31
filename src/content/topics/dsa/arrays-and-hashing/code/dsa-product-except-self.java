// out[i] = product of every element except nums[i], without division, in O(n).
int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] out = new int[n];
    out[0] = 1;
    // product of everything left of i
    for (int i = 1; i < n; i++) out[i] = out[i - 1] * nums[i - 1];
    // product of everything right of i
    int right = 1;
    for (int i = n - 1; i >= 0; i--) {
        out[i] *= right;
        right *= nums[i];
    }
    return out;
}
// [1,2,3,4] → [24,12,8,6]. Works with zeros (division would not). O(n) time, O(1) extra
// space.

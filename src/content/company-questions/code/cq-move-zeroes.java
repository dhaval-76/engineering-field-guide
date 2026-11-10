void moveZeroes(int[] nums) {
    int write = 0; // next place for a non-zero value
    for (int read = 0; read < nums.length; read++) {
        if (nums[read] != 0) {
            int t = nums[write]; // swap keeps it in place and O(1) space
            nums[write++] = nums[read];
            nums[read] = t;
        }
    }
}

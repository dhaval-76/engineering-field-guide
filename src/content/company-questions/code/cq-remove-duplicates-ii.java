int removeDuplicates(int[] nums) {
    int write = 0;
    for (int n : nums) {
        // Keep n unless two copies of it are already kept.
        if (write < 2 || nums[write - 2] != n) nums[write++] = n;
    }
    return write;
}

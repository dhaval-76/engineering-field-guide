boolean find132pattern(int[] nums) {
    int third = Integer.MIN_VALUE; // the best "2", always smaller than some "3" to its left
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = nums.length - 1; i >= 0; i--) {
        if (nums[i] < third) return true;
        while (!stack.isEmpty() && stack.peek() < nums[i]) third = stack.pop();
        stack.push(nums[i]);
    }
    return false;
}

int maximumUniqueSubarray(int[] nums) {
    Set<Integer> inWindow = new HashSet<>();
    int left = 0, sum = 0, best = 0;
    for (int n : nums) {
        while (inWindow.contains(n)) { // drop from the left until n is unique again
            inWindow.remove(nums[left]);
            sum -= nums[left++];
        }
        inWindow.add(n);
        sum += n;
        best = Math.max(best, sum);
    }
    return best;
}

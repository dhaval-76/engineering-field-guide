List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> out = new ArrayList<>();
    collectSubsets(nums, 0, new ArrayList<>(), out);
    return out;
}
private void collectSubsets(int[] nums, int start, List<Integer> path, List<List<Integer>> out) {
    // every node of the tree is a subset
    out.add(new ArrayList<>(path));
    for (int i = start; i < nums.length; i++) {
        path.add(nums[i]);
        // only later elements: no reordered duplicates
        collectSubsets(nums, i + 1, path, out);
        path.remove(path.size() - 1);
    }
}

List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> out = new ArrayList<>();
    collectPermutations(nums, new boolean[nums.length], new ArrayList<>(), out);
    return out;
}
private void collectPermutations(int[] nums, boolean[] used, List<Integer> path, List<List<Integer>> out) {
    if (path.size() == nums.length) { out.add(new ArrayList<>(path)); return; }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        used[i] = true; path.add(nums[i]);
        collectPermutations(nums, used, path, out);
        used[i] = false; path.remove(path.size() - 1);
    }
}

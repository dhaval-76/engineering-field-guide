// All distinct subsets when the input has duplicates: sort, then at each depth skip a
// value equal to the previous sibling — choosing either copy first leads to the same
// subsets.
List<List<Integer>> subsetsWithDup(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> out = new ArrayList<>();
    build(nums, 0, new ArrayList<>(), out);
    return out;
}

void build(int[] nums, int start, List<Integer> path, List<List<Integer>> out) {
    out.add(new ArrayList<>(path));                  // every node of the tree is a subset
    for (int i = start; i < nums.length; i++) {
        if (i > start && nums[i] == nums[i - 1]) continue;   // duplicate sibling: skip
        path.add(nums[i]);                           // choose
        build(nums, i + 1, path, out);               // explore
        path.remove(path.size() - 1);                // unchoose
    }
}
// [1,2,2] → [], [1], [1,2], [1,2,2], [2], [2,2] — 6 subsets, not 8.

List<List<Integer>> combinationSum(int[] candidates, int target) {
    int[] sorted = candidates.clone();
    // enables the early break below
    Arrays.sort(sorted);
    List<List<Integer>> out = new ArrayList<>();
    search(sorted, target, 0, new ArrayList<>(), out);
    return out;
}

private void search(int[] c, int remaining, int start,
                    List<Integer> path, List<List<Integer>> out) {
    if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
    for (int i = start; i < c.length; i++) {
        // prune: every later candidate is larger too
        if (c[i] > remaining) break;
        path.add(c[i]);
        // i, not i + 1: reuse allowed
        search(c, remaining - c[i], i, path, out);
        path.remove(path.size() - 1);
    }
}
// [2,3,6,7], 7 → [[2,2,3],[7]]

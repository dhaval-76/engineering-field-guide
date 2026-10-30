List<List<Integer>> combinationSum(int[] candidates, int target) {
    int[] sorted = candidates.clone();
    Arrays.sort(sorted);                                       // enables the early break below
    List<List<Integer>> out = new ArrayList<>();
    search(sorted, target, 0, new ArrayList<>(), out);
    return out;
}

private void search(int[] c, int remaining, int start, List<Integer> path, List<List<Integer>> out) {
    if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
    for (int i = start; i < c.length; i++) {
        if (c[i] > remaining) break;                           // prune: every later candidate is larger too
        path.add(c[i]);
        search(c, remaining - c[i], i, path, out);             // i, not i + 1: reuse allowed
        path.remove(path.size() - 1);
    }
}
// [2,3,6,7], 7 → [[2,2,3],[7]]

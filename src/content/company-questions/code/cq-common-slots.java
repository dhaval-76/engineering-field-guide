/** Each person's free slots are sorted and don't overlap. Returns when all are free. */
List<int[]> commonSlots(List<List<int[]>> people) {
    List<int[]> common = people.get(0);
    for (int p = 1; p < people.size(); p++) common = intersect(common, people.get(p));
    return common;
}

List<int[]> intersect(List<int[]> a, List<int[]> b) {
    List<int[]> out = new ArrayList<>();
    int i = 0, j = 0;
    while (i < a.size() && j < b.size()) {
        int start = Math.max(a.get(i)[0], b.get(j)[0]);
        int end = Math.min(a.get(i)[1], b.get(j)[1]);
        if (start < end) out.add(new int[] {start, end});
        // The slot that ends first can't overlap anything later.
        if (a.get(i)[1] < b.get(j)[1]) i++; else j++;
    }
    return out;
}

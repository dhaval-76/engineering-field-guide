/** Merge half-open [start, end) slots that overlap or touch. Input may be unsorted; not mutated. */
List<int[]> mergeSlots(List<int[]> slots) {
    List<int[]> sorted = new ArrayList<>();
    // copy: don't touch caller's data
    for (int[] s : slots) sorted.add(new int[] {s[0], s[1]});
    sorted.sort(Comparator.comparingInt(s -> s[0]));

    List<int[]> out = new ArrayList<>();
    for (int[] s : sorted) {
        int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
        // overlap or touch
        if (last != null && s[0] <= last[1]) last[1] = Math.max(last[1], s[1]);
        else out.add(s);
    }
    return out;
}

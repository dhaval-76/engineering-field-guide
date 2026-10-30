int longestConsecutive(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int x : nums) set.add(x);
    int best = 0;
    for (int x : set) {
        if (set.contains(x - 1)) continue;                 // not the start of a run
        int length = 1;
        while (set.contains(x + length)) length++;
        best = Math.max(best, length);
    }
    return best;
}
// O(n): each number is the start of at most one scan, and each is scanned at most once

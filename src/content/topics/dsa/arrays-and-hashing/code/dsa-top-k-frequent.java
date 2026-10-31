// The k most frequent values in O(n): count, then bucket values by their frequency.
List<Integer> topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) freq.merge(x, 1, Integer::sum);
    // buckets[f] = values that occur exactly f times (f is at most n)
    List<List<Integer>> buckets = new ArrayList<>();
    for (int i = 0; i <= nums.length; i++) buckets.add(new ArrayList<>());
    freq.forEach((value, f) -> buckets.get(f).add(value));
    List<Integer> out = new ArrayList<>();
    for (int f = nums.length; f > 0 && out.size() < k; f--)
        for (int value : buckets.get(f)) if (out.size() < k) out.add(value);
    return out;
}
// [1,1,1,2,2,3], k = 2 → [1, 2]. O(n) time and space (a heap would be O(n log k)).

// Frequency count
Map<String, Long> freq = words.stream()
        .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

// Group by a key
Map<Status, List<Order>> byStatus = orders.stream().collect(Collectors.groupingBy(Order::status));

// Top K largest with a min-heap of size K: O(n log k)
static List<Integer> topK(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();
    for (int n : nums) {
        heap.offer(n);
        if (heap.size() > k) heap.poll();            // drop the smallest
    }
    List<Integer> out = new ArrayList<>(heap);
    out.sort(Comparator.reverseOrder());
    return out;
}

// First non-repeating character: LinkedHashMap keeps first-seen order
static Optional<Character> firstUnique(String s) {
    Map<Character, Integer> counts = new LinkedHashMap<>();
    for (char c : s.toCharArray()) counts.merge(c, 1, Integer::sum);
    return counts.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
            .findFirst();
}

// 1. Seen set: "have I met this before?"
boolean containsDuplicate(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    // add returns false if already present
    for (int x : nums) if (!seen.add(x)) return true;
    return false;
}

// 2. Frequency map: "how many of each?"
Map<Character, Integer> counts(String s) {
    Map<Character, Integer> freq = new HashMap<>();
    for (char c : s.toCharArray()) freq.merge(c, 1, Integer::sum);
    return freq;
}
// For lowercase letters, an int[26] is faster: freq[c - 'a']++

// 3. Grouping by a canonical key
Map<String, List<String>> groupBy(List<String> items, Function<String, String> key) {
    Map<String, List<String>> groups = new HashMap<>();
    for (String it : items) {
        groups.computeIfAbsent(key.apply(it), k -> new ArrayList<>()).add(it);
    }
    return groups;
}

// 4. Prefix sums: sum of nums[i..j] in O(1) after O(n) preprocessing
long[] prefix(int[] nums) {
    long[] p = new long[nums.length + 1];
    for (int i = 0; i < nums.length; i++) p[i + 1] = p[i] + nums[i];
    return p;                                  // sum(i..j) = p[j + 1] - p[i]
}

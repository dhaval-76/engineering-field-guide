List<List<String>> groupAnagrams(String[] words) {
    Map<String, List<String>> groups = new HashMap<>();
    for (String w : words) {
        int[] count = new int[26];
        for (char c : w.toCharArray()) count[c - 'a']++;
        // e.g. "[1, 0, 0, 1, ...]" — O(k), no sorting
        String key = Arrays.toString(count);
        groups.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
    }
    return new ArrayList<>(groups.values());
}
// O(n · k) time, O(n · k) space (n words of length k)

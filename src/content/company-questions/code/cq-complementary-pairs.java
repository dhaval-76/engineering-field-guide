long complementaryPairs(String[] words) {
    Map<Integer, Integer> seen = new HashMap<>();
    long pairs = 0;
    for (String w : words) {
        int mask = 0;
        for (char c : w.toCharArray()) mask ^= 1 << (c - 'a'); // bit set = odd count
        pairs += seen.getOrDefault(mask, 0);
        for (int b = 0; b < 26; b++) pairs += seen.getOrDefault(mask ^ (1 << b), 0);
        seen.merge(mask, 1, Integer::sum);
    }
    return pairs;
}

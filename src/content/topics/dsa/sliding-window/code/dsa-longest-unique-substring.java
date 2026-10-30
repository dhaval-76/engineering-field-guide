int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> lastSeen = new HashMap<>();
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        Integer prev = lastSeen.get(c);
        if (prev != null && prev >= left) left = prev + 1;   // jump past the duplicate
        lastSeen.put(c, right);
        best = Math.max(best, right - left + 1);
    }
    return best;
}
// "abcabcbb" → 3, "bbbbb" → 1, "pwwkew" → 3. O(n) time, O(alphabet) space.

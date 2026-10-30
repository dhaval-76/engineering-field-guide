String minWindow(String s, String t) {
    int[] need = new int[128];
    for (char c : t.toCharArray()) need[c]++;
    int missing = t.length();                          // characters of t still not covered
    int left = 0, bestStart = 0, bestLen = Integer.MAX_VALUE;
    for (int right = 0; right < s.length(); right++) {
        if (need[s.charAt(right)]-- > 0) missing--;     // this char was still needed
        while (missing == 0) {                         // valid: try to shrink
            if (right - left + 1 < bestLen) { bestLen = right - left + 1; bestStart = left; }
            if (++need[s.charAt(left++)] > 0) missing++;   // removed a needed char → invalid
        }
    }
    return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
}
// minWindow("ADOBECODEBANC", "ABC") → "BANC". O(|s| + |t|).

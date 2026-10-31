int characterReplacement(String s, int k) {
    int[] count = new int[26];
    int left = 0, maxFreq = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        maxFreq = Math.max(maxFreq, ++count[s.charAt(right) - 'A']);
        // letters to replace = window length − most frequent letter
        while (right - left + 1 - maxFreq > k) count[s.charAt(left++) - 'A']--;
        best = Math.max(best, right - left + 1);
    }
    return best;
}
// maxFreq may be stale (too high) after shrinking; that only stops the window from
// growing, never yields a wrong larger answer. "AABABBA", k = 1 → 4.

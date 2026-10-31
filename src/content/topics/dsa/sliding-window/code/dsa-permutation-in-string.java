// Does s2 contain a permutation (anagram) of s1? Fixed window of length |s1| with letter counts.
boolean checkInclusion(String s1, String s2) {
    if (s1.length() > s2.length()) return false;
    int[] diff = new int[26];              // count in window − count in s1
    for (char c : s1.toCharArray()) diff[c - 'a']--;
    int mismatched = 0;                    // letters whose count differs
    for (int d : diff) if (d != 0) mismatched++;
    for (int right = 0; right < s2.length(); right++) {
        mismatched += change(diff, s2.charAt(right) - 'a', +1);             // enters
        if (right >= s1.length())
            mismatched += change(diff, s2.charAt(right - s1.length()) - 'a', -1); // leaves
        if (mismatched == 0) return true;
    }
    return false;
}

// Applies one count change and returns how the number of mismatched letters changed.
int change(int[] diff, int letter, int delta) {
    int before = diff[letter];
    diff[letter] += delta;
    return (diff[letter] != 0 ? 1 : 0) - (before != 0 ? 1 : 0);
}
// checkInclusion("ab", "eidbaooo") → true ("ba"); ("ab", "eidboaoo") → false. O(|s2|).

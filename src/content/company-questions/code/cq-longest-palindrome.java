String longestPalindrome(String s) {
    int bestStart = 0, bestLength = 0;
    for (int centre = 0; centre < 2 * s.length() - 1; centre++) {
        int left = centre / 2, right = left + centre % 2; // odd and even centres
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        if (right - left - 1 > bestLength) {
            bestLength = right - left - 1;
            bestStart = left + 1;
        }
    }
    return s.substring(bestStart, bestStart + bestLength);
}

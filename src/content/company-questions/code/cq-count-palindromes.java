int countPalindromes(String s) {
    int count = 0;
    for (int centre = 0; centre < 2 * s.length() - 1; centre++) {
        int left = centre / 2, right = left + centre % 2; // odd and even centres
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            count++;
            left--;
            right++;
        }
    }
    return count;
}

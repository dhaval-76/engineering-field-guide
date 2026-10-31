boolean isPalindrome(String s) {
    int l = 0, r = s.length() - 1;
    while (l < r) {
        if (!Character.isLetterOrDigit(s.charAt(l))) { l++; continue; }
        if (!Character.isLetterOrDigit(s.charAt(r))) { r--; continue; }
        char a = Character.toLowerCase(s.charAt(l)), b = Character.toLowerCase(s.charAt(r));
        if (a != b) return false;
        l++; r--;
    }
    return true;
}
// "A man, a plan, a canal: Panama" → true. O(n) time, O(1) space.

String nextPalindrome(String s) {
    int n = s.length();
    String left = s.substring(0, (n + 1) / 2);
    String candidate = mirror(left, n);
    if (candidate.compareTo(s) > 0) return candidate; // same length, so text order works
    String next = new java.math.BigInteger(left).add(java.math.BigInteger.ONE).toString();
    if (next.length() > left.length()) return "1" + "0".repeat(n - 1) + "1"; // 999 -> 1001
    return mirror(next, n);
}

String mirror(String left, int n) {
    return left + new StringBuilder(left.substring(0, n / 2)).reverse();
}

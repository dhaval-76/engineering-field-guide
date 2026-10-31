/** dp[i][j] = LCS length of a[0..i) and b[0..j). */
int longestCommonSubsequence(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 1; i <= a.length(); i++)
        for (int j = 1; j <= b.length(); j++)
            dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                ? dp[i - 1][j - 1] + 1                        // extend the match
                : Math.max(dp[i - 1][j], dp[i][j - 1]);       // drop a char from one side
    return dp[a.length()][b.length()];
}

/** dp[i][j] = minimum edits (insert, delete, replace) turning a[0..i) into b[0..j). */
int minDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) dp[i][0] = i;       // delete everything
    for (int j = 0; j <= b.length(); j++) dp[0][j] = j;       // insert everything
    for (int i = 1; i <= a.length(); i++)
        for (int j = 1; j <= b.length(); j++)
            dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                ? dp[i - 1][j - 1]
                : 1 + Math.min(dp[i - 1][j - 1],                 // replace
                      Math.min(dp[i - 1][j], dp[i][j - 1]));     // delete, insert
    return dp[a.length()][b.length()];
}
// LCS("abcde", "ace") = 3; edit("horse", "ros") = 3. O(n·m) time and space (O(min) with
// two rows).

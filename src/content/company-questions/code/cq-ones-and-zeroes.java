int findMaxForm(String[] strs, int maxZeros, int maxOnes) {
    int[][] dp = new int[maxZeros + 1][maxOnes + 1];
    for (String s : strs) {
        int zeros = (int) s.chars().filter(c -> c == '0').count();
        int ones = s.length() - zeros;
        // Backwards, so each string is used at most once.
        for (int z = maxZeros; z >= zeros; z--) {
            for (int o = maxOnes; o >= ones; o--) {
                dp[z][o] = Math.max(dp[z][o], dp[z - zeros][o - ones] + 1);
            }
        }
    }
    return dp[maxZeros][maxOnes];
}

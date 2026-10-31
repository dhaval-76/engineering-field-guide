// Children in a row with ratings; each gets ≥ 1 candy, and a child rated higher than a
// neighbour gets more than that neighbour. Minimum total candies.
int candy(int[] ratings) {
    int n = ratings.length;
    int[] give = new int[n];
    Arrays.fill(give, 1);
    for (int i = 1; i < n; i++)                  // pass 1: satisfy every LEFT neighbour
        if (ratings[i] > ratings[i - 1]) give[i] = give[i - 1] + 1;
    for (int i = n - 2; i >= 0; i--)             // pass 2: satisfy every RIGHT neighbour,
        if (ratings[i] > ratings[i + 1])         //   without breaking pass 1
            give[i] = Math.max(give[i], give[i + 1] + 1);
    return Arrays.stream(give).sum();
}
// [1,0,2] → 5 (2,1,2); [1,2,2] → 4 (1,2,1). O(n) time, O(n) space.

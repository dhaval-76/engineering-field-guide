// Children in a row with ratings; each gets ≥ 1 candy, and a child rated higher than a
// neighbour gets more than that neighbour. Minimum total candies.
function candy(ratings) {
  const n = ratings.length;
  const give = new Array(n).fill(1);
  // pass 1: satisfy every LEFT neighbour
  for (let i = 1; i < n; i++) if (ratings[i] > ratings[i - 1]) give[i] = give[i - 1] + 1;
  // pass 2: satisfy every RIGHT neighbour, without breaking pass 1
  for (let i = n - 2; i >= 0; i--) {
    if (ratings[i] > ratings[i + 1]) give[i] = Math.max(give[i], give[i + 1] + 1);
  }
  return give.reduce((a, b) => a + b, 0);
}
// [1,0,2] → 5 (2,1,2); [1,2,2] → 4 (1,2,1). O(n) time, O(n) space.

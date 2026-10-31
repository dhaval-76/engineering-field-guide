# Children in a row with ratings; each gets ≥ 1 candy, and a child rated higher than a
# neighbour gets more than that neighbour. Minimum total candies.
def candy(ratings: list[int]) -> int:
    n = len(ratings)
    give = [1] * n
    for i in range(1, n):                    # pass 1: satisfy every LEFT neighbour
        if ratings[i] > ratings[i - 1]:
            give[i] = give[i - 1] + 1
    for i in range(n - 2, -1, -1):           # pass 2: satisfy every RIGHT neighbour,
        if ratings[i] > ratings[i + 1]:      #   without breaking pass 1
            give[i] = max(give[i], give[i + 1] + 1)
    return sum(give)
# [1,0,2] → 5 (2,1,2); [1,2,2] → 4 (1,2,1). O(n) time, O(n) space.

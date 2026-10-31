# Before trusting a greedy rule, compare it with an exact answer on many small inputs.
# Example: "largest coin first" for coin change. Returns the first amount where greedy
# uses more coins than the optimum (from DP), or -1 if none up to the limit.
def first_greedy_failure(coins: list[int], limit: int) -> int:
    coins = sorted(coins)
    inf = float("inf")
    best = [0] + [inf] * limit               # DP: fewest coins for each amount
    for a in range(1, limit + 1):
        best[a] = min((best[a - c] + 1 for c in coins if c <= a), default=inf)
    for a in range(1, limit + 1):
        left, used = a, 0
        for c in reversed(coins):            # greedy: largest coin first
            used += left // c
            left %= c
        if left == 0 and used > best[a]:     # greedy works, but uses more coins
            return a
    return -1
# [1,3,4] → 6 (greedy 4+1+1, optimal 3+3). [1,2,5,10,20,50,100,200,500,2000] → -1:
# real currencies are designed so greedy works ("canonical" coin systems).

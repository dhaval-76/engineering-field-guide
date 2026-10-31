# Median of two sorted arrays in O(log(min(m, n))): binary search for a split of the
# shorter array such that everything on the left of both splits ≤ everything on the right.
def find_median_sorted_arrays(a: list[int], b: list[int]) -> float:
    if len(a) > len(b):
        a, b = b, a
    m, n = len(a), len(b)
    half = (m + n + 1) // 2
    lo, hi = 0, m
    inf = float("inf")
    while lo <= hi:
        i = (lo + hi) // 2          # elements taken from a
        j = half - i                # elements taken from b
        a_left = a[i - 1] if i > 0 else -inf
        a_right = a[i] if i < m else inf
        b_left = b[j - 1] if j > 0 else -inf
        b_right = b[j] if j < n else inf
        if a_left <= b_right and b_left <= a_right:
            left_max = max(a_left, b_left)
            if (m + n) % 2:
                return float(left_max)
            return (left_max + min(a_right, b_right)) / 2
        if a_left > b_right:
            hi = i - 1              # took too many from a
        else:
            lo = i + 1              # took too few from a
    raise ValueError("inputs must be sorted")
# [1,3] and [2] → 2.0; [1,2] and [3,4] → 2.5

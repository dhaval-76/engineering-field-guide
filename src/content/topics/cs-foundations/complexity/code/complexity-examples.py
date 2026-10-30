# O(n) time, O(n) space
def has_duplicate(a: list[int]) -> bool:
    seen = set()
    for x in a:
        if x in seen:
            return True
        seen.add(x)
    return False

# O(n log n): sorting dominates
def has_duplicate_sorted(a: list[int]) -> bool:
    s = sorted(a)
    return any(s[i] == s[i - 1] for i in range(1, len(s)))

# Hidden costs: `x in list` is O(n); slicing a[1:] copies (O(n)) — avoid both inside loops
def unique_slow(xs):
    out = []
    for x in xs:
        if x not in out:        # O(n) each → O(n²) overall; use dict.fromkeys(xs) → O(n)
            out.append(x)
    return out

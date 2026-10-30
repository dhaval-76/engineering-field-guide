from collections import Counter

def min_window(s: str, t: str) -> str:
    need = Counter(t)
    missing = len(t)
    left = best_start = 0
    best_len = float('inf')
    for right, c in enumerate(s):
        if need[c] > 0:
            missing -= 1
        need[c] -= 1
        while missing == 0:
            if right - left + 1 < best_len:
                best_start, best_len = left, right - left + 1
            need[s[left]] += 1
            if need[s[left]] > 0:
                missing += 1
            left += 1
    return "" if best_len == float('inf') else s[best_start:best_start + best_len]

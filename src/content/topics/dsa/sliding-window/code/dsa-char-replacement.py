def character_replacement(s: str, k: int) -> int:
    count = [0] * 26
    left = max_freq = best = 0
    for right, c in enumerate(s):
        count[ord(c) - 65] += 1
        max_freq = max(max_freq, count[ord(c) - 65])
        while right - left + 1 - max_freq > k:
            count[ord(s[left]) - 65] -= 1
            left += 1
        best = max(best, right - left + 1)
    return best

# Does s2 contain a permutation (anagram) of s1? Fixed window of length |s1| with letter
# counts.
def check_inclusion(s1: str, s2: str) -> bool:
    if len(s1) > len(s2):
        return False
    diff = [0] * 26                      # count in window − count in s1
    for c in s1:
        diff[ord(c) - 97] -= 1
    mismatched = sum(d != 0 for d in diff)

    def change(letter: int, delta: int) -> int:
        before = diff[letter]
        diff[letter] += delta
        return (diff[letter] != 0) - (before != 0)

    for right, c in enumerate(s2):
        mismatched += change(ord(c) - 97, +1)                       # enters
        if right >= len(s1):
            mismatched += change(ord(s2[right - len(s1)]) - 97, -1)  # leaves
        if mismatched == 0:
            return True
    return False
# check_inclusion("ab", "eidbaooo") → True ("ba"); ("ab", "eidboaoo") → False. O(|s2|).

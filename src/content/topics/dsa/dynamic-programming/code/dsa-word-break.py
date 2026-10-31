# Can s be split into a sequence of dictionary words?
# ok[i] = the prefix s[:i] can be split. ok[i] is true if some word ends exactly at i
# and the text before that word can be split.
def word_break(s: str, dictionary: list[str]) -> bool:
    words = set(dictionary)
    longest = max(map(len, dictionary), default=0)
    ok = [True] + [False] * len(s)           # ok[0]: the empty prefix
    for i in range(1, len(s) + 1):
        ok[i] = any(ok[j] and s[j:i] in words for j in range(max(0, i - longest), i))
    return ok[len(s)]
# "leetcode" with [leet, code] → True; "catsandog" with [cats,dog,sand,and,cat] → False.

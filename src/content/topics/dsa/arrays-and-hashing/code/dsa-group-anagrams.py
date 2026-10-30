from collections import defaultdict

def group_anagrams(words: list[str]) -> list[list[str]]:
    groups = defaultdict(list)
    for w in words:
        count = [0] * 26
        for c in w:
            count[ord(c) - ord('a')] += 1
        groups[tuple(count)].append(w)          # tuples are hashable; lists are not
    return list(groups.values())

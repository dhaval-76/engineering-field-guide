from string import ascii_lowercase


# Fewest words in a chain begin → end, changing one letter at a time, every word in the
# list. The graph is implicit: neighbours are generated, not stored. BFS gives the fewest
# steps.
def ladder_length(begin: str, end: str, word_list: list[str]) -> int:
    words = set(word_list)
    if end not in words:
        return 0
    level = [begin]
    words.discard(begin)                       # discarding = marking visited
    length = 1
    while level:
        nxt = []
        for word in level:
            for i in range(len(word)):
                for c in ascii_lowercase:
                    candidate = word[:i] + c + word[i + 1:]
                    if candidate not in words:
                        continue                  # not a word, or already seen
                    if candidate == end:
                        return length + 1
                    words.discard(candidate)
                    nxt.append(candidate)
        level, length = nxt, length + 1
    return 0
# "hit" → "cog" with [hot,dot,dog,lot,log,cog] → 5 (hit hot dot dog cog). O(N · L · 26).

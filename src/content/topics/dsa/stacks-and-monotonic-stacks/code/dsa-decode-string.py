# "3[a2[c]]" → "accaccacc". Each '[' saves the outer state; each ']' finishes a level.
def decode_string(s: str) -> str:
    stack: list[tuple[list[str], int]] = []   # (text before '[', repeat count)
    current: list[str] = []
    k = 0
    for c in s:
        if c.isdigit():
            k = k * 10 + int(c)                # counts can have several digits
        elif c == "[":
            stack.append((current, k))
            current, k = [], 0
        elif c == "]":
            before, count = stack.pop()
            before.append("".join(current) * count)
            current = before                   # back to the enclosing level
        else:
            current.append(c)
    return "".join(current)
# "3[a]2[bc]" → "aaabcbc", "2[abc]3[cd]ef" → "abcabccdcdcdef". O(output length).

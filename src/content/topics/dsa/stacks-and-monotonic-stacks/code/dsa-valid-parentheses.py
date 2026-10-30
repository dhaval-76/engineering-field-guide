def is_valid(s: str) -> bool:
    opener_for = {')': '(', ']': '[', '}': '{'}
    stack = []
    for c in s:
        if c in opener_for:
            if not stack or stack.pop() != opener_for[c]:
                return False
        else:
            stack.append(c)
    return not stack

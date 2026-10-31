# All balanced strings of n pairs. Prune instead of generating all 2^(2n) strings:
# add '(' while some are left; add ')' only while it would close an open one.
def generate_parenthesis(n: int) -> list[str]:
    out: list[str] = []

    def generate(s: str, open_: int, close: int) -> None:
        if len(s) == 2 * n:
            out.append(s)
            return
        if open_ < n:
            generate(s + "(", open_ + 1, close)
        if close < open_:                        # never more ')' than '(' so far
            generate(s + ")", open_, close + 1)

    generate("", 0, 0)
    return out
# n = 3 → ((())), (()()), (())(), ()(()), ()()(). The count is the Catalan number C(n).

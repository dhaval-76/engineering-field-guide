def rank_between(before: str, after: str | None) -> str:
    """A rank strictly between two ranks, using lowercase letters as base-26 digits.
    after=None means "end of column". Generated ranks never end in 'a', so there is always room."""
    out = []
    i = 0
    while True:
        lo = ord(before[i]) - 97 if i < len(before) else 0
        hi = ord(after[i]) - 97 if after is not None and i < len(after) else 26
        # room at this position: take the midpoint
        if hi - lo > 1:
            out.append(chr(97 + (lo + hi) // 2))
            return "".join(out)
        # no room: keep this digit and look deeper
        out.append(chr(97 + lo))
        if hi - lo == 1:
            # now below `after`, so the upper bound opens up
            after = None
        i += 1
# rank_between("c", "m") → "h";  ("h", "i") → "hn";  ("", "b") → "an";  ("t", None) → "w"

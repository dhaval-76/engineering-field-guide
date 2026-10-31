def rank_between(before: str, after: str | None) -> str:
    """A rank strictly between two ranks, using lowercase letters as base-26 digits.
    after=None means "end of column". Generated ranks never end in 'a', so there is always room."""
    out = []
    i = 0
    while True:
        lo = ord(before[i]) - 97 if i < len(before) else 0
        hi = ord(after[i]) - 97 if after is not None and i < len(after) else 26
        if hi - lo > 1:                               # room at this position: take the midpoint
            out.append(chr(97 + (lo + hi) // 2))
            return "".join(out)
        out.append(chr(97 + lo))                      # no room: keep this digit and look deeper
        if hi - lo == 1:
            after = None                              # now below `after`, so the upper bound opens up
        i += 1
# rank_between("c", "m") → "h";  ("h", "i") → "hn";  ("", "b") → "an";  ("t", None) → "w"

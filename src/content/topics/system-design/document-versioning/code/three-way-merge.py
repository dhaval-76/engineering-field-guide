def merge(base: dict, ours: dict, theirs: dict) -> tuple[dict, list]:
    """Merge two edited versions of a document made of blocks with stable ids.
    A missing id means absent or deleted. Returns (merged blocks, conflicts)."""
    merged, conflicts = {}, []
    for bid in dict.fromkeys([*base, *ours, *theirs]):           # ordered union of ids
        b, o, t = base.get(bid), ours.get(bid), theirs.get(bid)
        if o == t:                                               # same on both sides
            if o is not None:
                merged[bid] = o
        elif o == b:                                             # only theirs changed
            if t is not None:
                merged[bid] = t
        elif t == b:                                             # only ours changed
            if o is not None:
                merged[bid] = o
        else:                                                    # both changed differently
            conflicts.append({"block_id": bid, "base": b, "ours": o, "theirs": t})
    return merged, conflicts                                     # real implementations also merge block ORDER

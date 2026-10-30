def backtrack(path, choices, results):
    if is_complete(path):
        results.append(path[:])                  # copy
        return
    for c in choices(path):
        if not valid(path, c):
            continue                             # prune
        path.append(c)                           # choose
        backtrack(path, choices, results)        # explore
        path.pop()                               # unchoose

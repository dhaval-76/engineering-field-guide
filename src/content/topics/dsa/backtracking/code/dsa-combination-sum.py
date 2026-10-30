def combination_sum(candidates, target):
    c, out, path = sorted(candidates), [], []
    def go(remaining, start):
        if remaining == 0:
            out.append(path[:])
            return
        for i in range(start, len(c)):
            if c[i] > remaining:
                break
            path.append(c[i])
            go(remaining - c[i], i)
            path.pop()
    go(target, 0)
    return out

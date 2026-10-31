# Two lists of closed intervals, each sorted and non-overlapping. Return their
# intersections.
def interval_intersection(a: list[list[int]], b: list[list[int]]) -> list[list[int]]:
    out = []
    i = j = 0
    while i < len(a) and j < len(b):
        lo = max(a[i][0], b[j][0])           # the later start
        hi = min(a[i][1], b[j][1])           # the earlier end
        if lo <= hi:
            out.append([lo, hi])
        # the interval that ends first can't meet anything else in the other list
        if a[i][1] < b[j][1]:
            i += 1
        else:
            j += 1
    return out
# a = [[0,2],[5,10],[13,23],[24,25]], b = [[1,5],[8,12],[15,24],[25,26]]
# → [[1,2],[5,5],[8,10],[15,23],[24,24],[25,25]]. O(m + n).

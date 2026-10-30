def insert(intervals, new):
    s, e = new
    before = [iv for iv in intervals if iv[1] < s]
    after = [iv for iv in intervals if iv[0] > e]
    overlapping = intervals[len(before):len(intervals) - len(after)]
    if overlapping:
        s, e = min(s, overlapping[0][0]), max(e, overlapping[-1][1])
    return before + [[s, e]] + after

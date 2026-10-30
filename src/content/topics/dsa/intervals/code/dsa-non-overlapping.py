def erase_overlap_intervals(intervals):
    kept, last_end = 0, float('-inf')
    for s, e in sorted(intervals, key=lambda iv: iv[1]):
        if s >= last_end:
            kept += 1
            last_end = e
    return len(intervals) - kept

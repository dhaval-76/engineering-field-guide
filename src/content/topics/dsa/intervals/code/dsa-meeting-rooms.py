import heapq

def min_meeting_rooms(meetings):
    ends = []
    for s, e in sorted(meetings):
        if ends and ends[0] <= s:
            heapq.heapreplace(ends, e)          # reuse the earliest-freed room
        else:
            heapq.heappush(ends, e)
    return len(ends)

def min_meeting_rooms_sweep(meetings):
    events = sorted([(s, 1) for s, _ in meetings] + [(e, -1) for _, e in meetings])
    rooms = best = 0
    for _, delta in events:                     # (t, -1) sorts before (t, +1): ends first
        rooms += delta
        best = max(best, rooms)
    return best

from collections import deque

class HitCounter:
    """Queue of (second, count): O(1) amortised, memory proportional to distinct seconds in the window."""
    WINDOW = 300

    def __init__(self):
        self.buckets = deque()
        self.total = 0

    def _evict(self, now):
        while self.buckets and now - self.buckets[0][0] >= self.WINDOW:
            self.total -= self.buckets.popleft()[1]

    def hit(self, timestamp):
        self._evict(timestamp)
        if self.buckets and self.buckets[-1][0] == timestamp:
            self.buckets[-1][1] += 1
        else:
            self.buckets.append([timestamp, 1])
        self.total += 1

    def get_hits(self, timestamp):
        self._evict(timestamp)
        return self.total

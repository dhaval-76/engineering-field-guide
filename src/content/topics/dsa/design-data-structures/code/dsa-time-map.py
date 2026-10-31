from bisect import bisect_right
from collections import defaultdict

class TimeMap:
    def __init__(self):
        self.times = defaultdict(list)
        self.values = defaultdict(list)

    def set(self, key, value, timestamp):
        self.times[key].append(timestamp)
        self.values[key].append(value)

    def get(self, key, t):
        # number of versions with timestamp ≤ t
        i = bisect_right(self.times[key], t)
        return self.values[key][i - 1] if i else ""

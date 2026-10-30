from collections import Counter, defaultdict
from itertools import accumulate

def contains_duplicate(nums):
    return len(set(nums)) != len(nums)

def counts(s):
    return Counter(s)

def group_by(items, key):
    groups = defaultdict(list)
    for it in items:
        groups[key(it)].append(it)
    return groups

def prefix(nums):
    return [0, *accumulate(nums)]               # sum(i..j) = p[j + 1] - p[i]

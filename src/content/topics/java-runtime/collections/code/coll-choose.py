from collections import deque, Counter, defaultdict
import heapq
from bisect import insort

history: list = []                 # ordered, indexed
seen_ids: set = set()              # membership O(1)
by_email: dict = {}                # insertion-ordered hash map
work = deque()                     # O(1) at both ends
jobs: list = []                    # heapq turns a list into a min-heap
heapq.heappush(jobs, (deadline, job_id))
counts = Counter(words)            # frequency map
sorted_scores: list = []
insort(sorted_scores, 42)          # keep a list sorted (O(n) insert, O(log n) search)

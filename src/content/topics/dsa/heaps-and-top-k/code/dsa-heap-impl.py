class MinHeap:
    def __init__(self):
        self.a = []

    def push(self, x):
        a = self.a
        a.append(x)
        i = len(a) - 1
        while i > 0 and a[(i - 1) // 2] > a[i]:              # sift up
            a[i], a[(i - 1) // 2] = a[(i - 1) // 2], a[i]
            i = (i - 1) // 2

    def pop(self):
        a = self.a
        top, last = a[0], a.pop()
        if a:
            a[0] = last
            i = 0
            while True:                                      # sift down
                l, r, smallest = 2 * i + 1, 2 * i + 2, i
                if l < len(a) and a[l] < a[smallest]:
                    smallest = l
                if r < len(a) and a[r] < a[smallest]:
                    smallest = r
                if smallest == i:
                    break
                a[i], a[smallest] = a[smallest], a[i]
                i = smallest
        return top
# In practice: heapq.heappush / heapq.heappop / heapq.heapify on a plain list.

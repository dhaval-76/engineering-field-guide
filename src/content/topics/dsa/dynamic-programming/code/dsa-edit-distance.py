def longest_common_subsequence(a, b):
    prev = [0] * (len(b) + 1)                   # two rows: O(len(b)) space
    for ca in a:
        cur = [0]
        for j, cb in enumerate(b, 1):
            cur.append(prev[j - 1] + 1 if ca == cb else max(prev[j], cur[j - 1]))
        prev = cur
    return prev[-1]

def min_distance(a, b):
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            if ca == cb:
                cur.append(prev[j - 1])
            else:
                cur.append(1 + min(prev[j - 1], prev[j], cur[j - 1]))
        prev = cur
    return prev[-1]

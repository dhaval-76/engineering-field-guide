from collections import deque

def alien_order(words: list[str]) -> str:
    nxt = {c: set() for w in words for c in w}
    in_degree = {c: 0 for c in nxt}
    for a, b in zip(words, words[1:]):
        if len(a) > len(b) and a.startswith(b):
            return ""
        for x, y in zip(a, b):
            if x != y:
                if y not in nxt[x]:
                    nxt[x].add(y)
                    in_degree[y] += 1
                break
    ready = deque(c for c, d in in_degree.items() if d == 0)
    order = []
    while ready:
        c = ready.popleft()
        order.append(c)
        for d in nxt[c]:
            in_degree[d] -= 1
            if in_degree[d] == 0:
                ready.append(d)
    return "".join(order) if len(order) == len(in_degree) else ""

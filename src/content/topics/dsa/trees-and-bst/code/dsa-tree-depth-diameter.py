def max_depth(n):
    return 1 + max(max_depth(n.left), max_depth(n.right)) if n else 0

def diameter(root):
    best = 0
    def height(n):
        nonlocal best
        if not n:
            return 0
        l, r = height(n.left), height(n.right)
        best = max(best, l + r)
        return 1 + max(l, r)
    height(root)
    return best

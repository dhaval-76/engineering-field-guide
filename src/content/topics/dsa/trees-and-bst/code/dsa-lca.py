def lowest_common_ancestor(n, p, q):
    if n is None or n is p or n is q:
        return n
    left = lowest_common_ancestor(n.left, p, q)
    right = lowest_common_ancestor(n.right, p, q)
    if left and right:
        return n
    return left or right

def lca_bst(n, p, q):
    while n:
        if p.val < n.val and q.val < n.val:
            n = n.left
        elif p.val > n.val and q.val > n.val:
            n = n.right
        else:
            return n

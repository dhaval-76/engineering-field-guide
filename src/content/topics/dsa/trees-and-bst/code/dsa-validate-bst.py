def is_valid_bst(n, low=float('-inf'), high=float('inf')):
    if not n:
        return True
    if not low < n.val < high:
        return False
    return is_valid_bst(n.left, low, n.val) and is_valid_bst(n.right, n.val, high)

def kth_smallest(root, k):
    stack, cur = [], root
    while True:
        while cur:
            stack.append(cur)
            cur = cur.left
        cur = stack.pop()
        k -= 1
        if k == 0:
            return cur.val
        cur = cur.right

# The largest sum of any path (any start and end node, no node twice). Values may be
# negative.
def max_path_sum(root: "TreeNode") -> int:
    best = float("-inf")

    # Returns the best sum of a path that starts at n and goes DOWN one side (so a parent
    # can extend it); meanwhile records the best path that bends at n (left + n + right).
    def gain(n: "TreeNode | None") -> int:
        nonlocal best
        if n is None:
            return 0
        left = max(0, gain(n.left))      # a negative branch is better left out
        right = max(0, gain(n.right))
        best = max(best, n.val + left + right)
        return n.val + max(left, right)

    gain(root)
    return best
# [-10,9,20,None,None,15,7] → 42 (15 → 20 → 7). O(n).

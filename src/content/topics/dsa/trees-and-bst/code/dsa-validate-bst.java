boolean isValidBST(TreeNode root) { return valid(root, null, null); }

/** Every node must lie strictly between the bounds inherited from ALL its ancestors. */
private boolean valid(TreeNode n, Integer low, Integer high) {
    if (n == null) return true;
    if ((low != null && n.val <= low) || (high != null && n.val >= high)) return false;
    return valid(n.left, low, n.val) && valid(n.right, n.val, high);
}

int kthSmallest(TreeNode root, int k) {          // in-order visits a BST in sorted order
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (true) {
        while (cur != null) { stack.push(cur); cur = cur.left; }
        cur = stack.pop();
        if (--k == 0) return cur.val;            // stop early: O(h + k)
        cur = cur.right;
    }
}

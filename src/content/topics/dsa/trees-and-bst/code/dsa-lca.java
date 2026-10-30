/** Any binary tree: the first node where p and q are found in different subtrees (or it is one of them). */
TreeNode lowestCommonAncestor(TreeNode n, TreeNode p, TreeNode q) {
    if (n == null || n == p || n == q) return n;
    TreeNode left = lowestCommonAncestor(n.left, p, q);
    TreeNode right = lowestCommonAncestor(n.right, p, q);
    if (left != null && right != null) return n;     // one on each side → n is the split point
    return left != null ? left : right;              // both on one side (or not found)
}

/** BST: walk down until p and q are on different sides. O(h), O(1) space. */
TreeNode lcaBst(TreeNode n, TreeNode p, TreeNode q) {
    while (n != null) {
        if (p.val < n.val && q.val < n.val) n = n.left;
        else if (p.val > n.val && q.val > n.val) n = n.right;
        else return n;
    }
    return null;
}

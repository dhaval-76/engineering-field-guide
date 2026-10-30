int maxDepth(TreeNode n) {
    return n == null ? 0 : 1 + Math.max(maxDepth(n.left), maxDepth(n.right));
}

private int bestDiameter;

int diameter(TreeNode root) {
    bestDiameter = 0;
    height(root);
    return bestDiameter;                          // in edges
}

/** Returns the height; records the longest path through each node on the way up. */
private int height(TreeNode n) {
    if (n == null) return 0;
    int l = height(n.left), r = height(n.right);
    bestDiameter = Math.max(bestDiameter, l + r); // path bending at n
    return 1 + Math.max(l, r);
}
// O(n): each node computes its height once (calling maxDepth inside a loop would be O(n²))

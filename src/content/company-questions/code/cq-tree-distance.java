int distance(TreeNode root, int a, int b) {
    TreeNode meet = lowestCommon(root, a, b);
    return depthOf(meet, a, 0) + depthOf(meet, b, 0);
}

TreeNode lowestCommon(TreeNode node, int a, int b) {
    if (node == null || node.val == a || node.val == b) return node;
    TreeNode l = lowestCommon(node.left, a, b), r = lowestCommon(node.right, a, b);
    if (l != null && r != null) return node;
    return l != null ? l : r;
}

int depthOf(TreeNode node, int value, int depth) {
    if (node == null) return -1;
    if (node.val == value) return depth;
    int left = depthOf(node.left, value, depth + 1);
    return left >= 0 ? left : depthOf(node.right, value, depth + 1);
}

int bestBstSum = 0;

int maxSumBST(TreeNode root) {
    bestBstSum = 0;
    bstInfo(root);
    return bestBstSum;
}

/** Returns {isBst, min, max, sum} for this subtree. */
int[] bstInfo(TreeNode node) {
    if (node == null) return new int[] {1, Integer.MAX_VALUE, Integer.MIN_VALUE, 0};
    int[] l = bstInfo(node.left), r = bstInfo(node.right);
    if (l[0] == 0 || r[0] == 0 || node.val <= l[2] || node.val >= r[1]) return new int[4];
    int sum = l[3] + r[3] + node.val;
    bestBstSum = Math.max(bestBstSum, sum);
    return new int[] {1, Math.min(l[1], node.val), Math.max(r[2], node.val), sum};
}

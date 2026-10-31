// The largest sum of any path (any start and end node, no node twice). Values may be
// negative.
int best;

int maxPathSum(TreeNode root) {
    best = Integer.MIN_VALUE;
    gain(root);
    return best;
}

// Returns the best sum of a path that starts at n and goes DOWN one side (so a parent can
// extend it); meanwhile records the best path that bends at n (left + n + right).
int gain(TreeNode n) {
    if (n == null) return 0;
    int left = Math.max(0, gain(n.left));     // a negative branch is better left out
    int right = Math.max(0, gain(n.right));
    best = Math.max(best, n.val + left + right);
    return n.val + Math.max(left, right);
}
// [-10,9,20,null,null,15,7] → 42 (15 → 20 → 7). O(n).

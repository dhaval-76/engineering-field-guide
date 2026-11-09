List<Integer> boundaryOfBinaryTree(TreeNode root) {
    List<Integer> out = new ArrayList<>(List.of(root.val));
    for (TreeNode n = root.left; n != null && !isLeaf(n); n = n.left != null ? n.left : n.right) {
        out.add(n.val);
    }
    if (!isLeaf(root)) leaves(root, out);
    List<Integer> right = new ArrayList<>();
    for (TreeNode n = root.right; n != null && !isLeaf(n); n = n.right != null ? n.right : n.left) {
        right.add(n.val);
    }
    Collections.reverse(right);
    out.addAll(right);
    return out;
}

boolean isLeaf(TreeNode n) {
    return n.left == null && n.right == null;
}

void leaves(TreeNode n, List<Integer> out) {
    if (n == null) return;
    if (isLeaf(n)) out.add(n.val);
    leaves(n.left, out);
    leaves(n.right, out);
}

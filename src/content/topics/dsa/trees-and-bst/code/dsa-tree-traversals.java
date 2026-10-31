void preorder(TreeNode n, List<Integer> out) {
    if (n == null) return;
    out.add(n.val); preorder(n.left, out); preorder(n.right, out);
}

void inorder(TreeNode n, List<Integer> out) {
    if (n == null) return;
    inorder(n.left, out); out.add(n.val); inorder(n.right, out);
}

/** Iterative in-order: avoids stack overflow on very deep trees. */
List<Integer> inorderIterative(TreeNode root) {
    List<Integer> out = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !stack.isEmpty()) {
        // go left as far as possible
        while (cur != null) { stack.push(cur); cur = cur.left; }
        cur = stack.pop();
        out.add(cur.val);                                          // visit
        // then the right subtree
        cur = cur.right;
    }
    return out;
}

List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> levels = new ArrayList<>();
    if (root == null) return levels;
    Deque<TreeNode> queue = new ArrayDeque<>(List.of(root));
    while (!queue.isEmpty()) {
        int size = queue.size();                                   // nodes on this level
        List<Integer> level = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TreeNode n = queue.poll();
            level.add(n.val);
            if (n.left != null) queue.offer(n.left);
            if (n.right != null) queue.offer(n.right);
        }
        levels.add(level);
    }
    return levels;
}

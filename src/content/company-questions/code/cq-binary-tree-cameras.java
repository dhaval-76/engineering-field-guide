int cameras = 0;

int minCameraCover(TreeNode root) {
    cameras = 0;
    if (visit(root) == 0) cameras++;
    return cameras;
}

/** 0 = needs covering, 1 = has a camera, 2 = covered with no camera. */
int visit(TreeNode node) {
    if (node == null) return 2;
    int left = visit(node.left), right = visit(node.right);
    if (left == 0 || right == 0) {
        cameras++;
        return 1;
    }
    return left == 1 || right == 1 ? 2 : 0;
}

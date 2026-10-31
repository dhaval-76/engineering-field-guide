// Pre-order with "#" for null children: the markers make the shape unambiguous, so one
// traversal is enough to rebuild the tree. [1,2,3,null,null,4,5] ↔
// "1,2,#,#,3,4,#,#,5,#,#"
String serialize(TreeNode root) {
    StringBuilder out = new StringBuilder();
    write(root, out);
    return out.toString();
}

void write(TreeNode n, StringBuilder out) {
    if (out.length() > 0) out.append(',');
    if (n == null) { out.append('#'); return; }
    out.append(n.val);
    write(n.left, out);
    write(n.right, out);
}

TreeNode deserialize(String data) {
    Iterator<String> tokens = Arrays.asList(data.split(",")).iterator();
    return read(tokens);
}

// Reads tokens in the same pre-order they were written: node, left subtree, right
// subtree.
TreeNode read(Iterator<String> tokens) {
    String t = tokens.next();
    if (t.equals("#")) return null;
    TreeNode n = new TreeNode(Integer.parseInt(t));
    n.left = read(tokens);
    n.right = read(tokens);
    return n;
}
// O(n) both ways. Without null markers, pre-order alone can't distinguish shapes.

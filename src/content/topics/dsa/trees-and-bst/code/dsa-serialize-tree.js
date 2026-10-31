// Pre-order with "#" for null children: the markers make the shape unambiguous, so one
// traversal is enough to rebuild the tree. [1,2,3,null,null,4,5] ↔
// "1,2,#,#,3,4,#,#,5,#,#"
function serialize(root) {
  const out = [];
  const write = (n) => {
    if (!n) return out.push('#');
    out.push(n.val);
    write(n.left);
    write(n.right);
  };
  write(root);
  return out.join(',');
}

function deserialize(data) {
  const tokens = data.split(',');
  let i = 0;
  // Reads tokens in the same pre-order they were written: node, left subtree, right
  // subtree.
  const read = () => {
    const t = tokens[i++];
    if (t === '#') return null;
    const n = new TreeNode(Number(t));
    n.left = read();
    n.right = read();
    return n;
  };
  return read();
}
// O(n) both ways. Without null markers, pre-order alone can't distinguish shapes.

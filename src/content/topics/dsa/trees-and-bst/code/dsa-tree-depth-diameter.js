const maxDepth = (n) => (n ? 1 + Math.max(maxDepth(n.left), maxDepth(n.right)) : 0);

function diameter(root) {
  let best = 0;
  const height = (n) => {
    if (!n) return 0;
    const l = height(n.left), r = height(n.right);
    best = Math.max(best, l + r);
    return 1 + Math.max(l, r);
  };
  height(root);
  return best;
}

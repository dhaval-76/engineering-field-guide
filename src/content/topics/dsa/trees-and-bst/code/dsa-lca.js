function lowestCommonAncestor(n, p, q) {
  if (!n || n === p || n === q) return n;
  const left = lowestCommonAncestor(n.left, p, q);
  const right = lowestCommonAncestor(n.right, p, q);
  return left && right ? n : left ?? right;
}

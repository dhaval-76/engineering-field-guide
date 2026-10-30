function isValidBST(n, low = -Infinity, high = Infinity) {
  if (!n) return true;
  if (n.val <= low || n.val >= high) return false;
  return isValidBST(n.left, low, n.val) && isValidBST(n.right, n.val, high);
}

function kthSmallest(root, k) {
  const stack = [];
  let cur = root;
  for (;;) {
    while (cur) { stack.push(cur); cur = cur.left; }
    cur = stack.pop();
    if (--k === 0) return cur.val;
    cur = cur.right;
  }
}

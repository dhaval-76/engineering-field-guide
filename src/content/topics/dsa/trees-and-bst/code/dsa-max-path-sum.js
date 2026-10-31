// The largest sum of any path (any start and end node, no node twice). Values may be
// negative.
function maxPathSum(root) {
  let best = -Infinity;
  // Returns the best sum of a path that starts at n and goes DOWN one side (so a parent
  // can extend it); meanwhile records the best path that bends at n (left + n + right).
  const gain = (n) => {
    if (!n) return 0;
    const left = Math.max(0, gain(n.left)); // a negative branch is better left out
    const right = Math.max(0, gain(n.right));
    best = Math.max(best, n.val + left + right);
    return n.val + Math.max(left, right);
  };
  gain(root);
  return best;
}
// [-10,9,20,null,null,15,7] → 42 (15 → 20 → 7). O(n).

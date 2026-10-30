function* inorder(n) {                 // generators make traversals lazy
  if (!n) return;
  yield* inorder(n.left);
  yield n.val;
  yield* inorder(n.right);
}

function levelOrder(root) {
  const levels = [];
  let level = root ? [root] : [];
  while (level.length) {
    levels.push(level.map((n) => n.val));
    level = level.flatMap((n) => [n.left, n.right].filter(Boolean));
  }
  return levels;
}

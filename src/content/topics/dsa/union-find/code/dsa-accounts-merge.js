// uses UnionFind from above
function accountsMerge(accounts) {
  const uf = new UnionFind(accounts.length);
  const owner = new Map();                         // email -> first account index that had it
  accounts.forEach(([, ...emails], i) => {
    for (const e of emails) {
      if (owner.has(e)) uf.union(i, owner.get(e));
      else owner.set(e, i);
    }
  });
  const groups = new Map();
  for (const [e, i] of owner) {
    const root = uf.find(i);
    if (!groups.has(root)) groups.set(root, []);
    groups.get(root).push(e);
  }
  return [...groups].map(([root, emails]) => [accounts[root][0], ...emails.sort()]);
}

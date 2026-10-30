function combinationSum(candidates, target) {
  const c = [...candidates].sort((a, b) => a - b), out = [], path = [];
  const go = (remaining, start) => {
    if (remaining === 0) return void out.push([...path]);
    for (let i = start; i < c.length && c[i] <= remaining; i++) {
      path.push(c[i]);
      go(remaining - c[i], i);
      path.pop();
    }
  };
  go(target, 0);
  return out;
}

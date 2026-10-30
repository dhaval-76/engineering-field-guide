const containsDuplicate = (nums) => new Set(nums).size !== nums.length;

function counts(s) {
  const freq = new Map();
  for (const c of s) freq.set(c, (freq.get(c) ?? 0) + 1);
  return freq;
}

function groupBy(items, key) {
  const groups = new Map();
  for (const it of items) {
    const k = key(it);
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(it);
  }
  return groups;
}

function prefix(nums) {
  const p = [0];
  for (const x of nums) p.push(p[p.length - 1] + x);
  return p;                                   // sum(i..j) = p[j + 1] - p[i]
}

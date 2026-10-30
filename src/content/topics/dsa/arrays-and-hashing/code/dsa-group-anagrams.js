function groupAnagrams(words) {
  const groups = new Map();
  for (const w of words) {
    const count = new Array(26).fill(0);
    for (const c of w) count[c.charCodeAt(0) - 97]++;
    const key = count.join(',');
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(w);
  }
  return [...groups.values()];
}

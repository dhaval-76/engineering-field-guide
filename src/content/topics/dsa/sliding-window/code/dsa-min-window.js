function minWindow(s, t) {
  const need = new Map();
  for (const c of t) need.set(c, (need.get(c) ?? 0) + 1);
  let missing = t.length, left = 0, bestStart = 0, bestLen = Infinity;
  for (let right = 0; right < s.length; right++) {
    const c = s[right];
    if ((need.get(c) ?? 0) > 0) missing--;
    need.set(c, (need.get(c) ?? 0) - 1);
    while (missing === 0) {
      if (right - left + 1 < bestLen) { bestLen = right - left + 1; bestStart = left; }
      const d = s[left++];
      need.set(d, need.get(d) + 1);
      if (need.get(d) > 0) missing++;
    }
  }
  return bestLen === Infinity ? '' : s.slice(bestStart, bestStart + bestLen);
}

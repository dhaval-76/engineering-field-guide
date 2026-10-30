function lengthOfLongestSubstring(s) {
  const lastSeen = new Map();
  let left = 0, best = 0;
  for (let right = 0; right < s.length; right++) {
    const prev = lastSeen.get(s[right]);
    if (prev !== undefined && prev >= left) left = prev + 1;
    lastSeen.set(s[right], right);
    best = Math.max(best, right - left + 1);
  }
  return best;
}

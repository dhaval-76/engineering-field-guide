// Does s2 contain a permutation (anagram) of s1? Fixed window of length |s1| with letter
// counts.
function checkInclusion(s1, s2) {
  if (s1.length > s2.length) return false;
  const diff = new Array(26).fill(0); // count in window − count in s1
  const idx = (c) => c.charCodeAt(0) - 97;
  for (const c of s1) diff[idx(c)]--;
  let mismatched = diff.filter((d) => d !== 0).length;
  const change = (letter, delta) => {
    const before = diff[letter];
    diff[letter] += delta;
    mismatched += (diff[letter] !== 0) - (before !== 0);
  };
  for (let right = 0; right < s2.length; right++) {
    change(idx(s2[right]), +1); // enters
    if (right >= s1.length) change(idx(s2[right - s1.length]), -1); // leaves
    if (mismatched === 0) return true;
  }
  return false;
}
// checkInclusion("ab", "eidbaooo") → true ("ba"); ("ab", "eidboaoo") → false. O(|s2|).

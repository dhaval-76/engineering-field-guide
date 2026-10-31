// O(n) time, O(n) space
function hasDuplicate(a) {
  const seen = new Set();
  for (const x of a) {
    if (seen.has(x)) return true;
    seen.add(x);
  }
  return false;
}

// O(n log n): sort dominates the linear scan (note: sort() needs a numeric comparator)
function hasDuplicateSorted(a) {
  const s = [...a].sort((x, y) => x - y);
  for (let i = 1; i < s.length; i++) if (s[i] === s[i - 1]) return true;
  return false;
}

// Hidden O(n²): includes() is O(n), and string += in a loop copies repeatedly in some
// engines
// use [...new Set(xs)] → O(n)
const uniqueSlow = (xs) => xs.filter((x, i) => xs.indexOf(x) === i);

// Can s be split into a sequence of dictionary words?
// ok[i] = the prefix s[0..i) can be split. ok[i] is true if some word ends exactly at i
// and the text before that word can be split.
function wordBreak(s, dictionary) {
  const words = new Set(dictionary);
  const longest = Math.max(0, ...dictionary.map((w) => w.length));
  const ok = new Array(s.length + 1).fill(false);
  ok[0] = true; // the empty prefix
  for (let i = 1; i <= s.length; i++) {
    for (let j = Math.max(0, i - longest); j < i && !ok[i]; j++) {
      ok[i] = ok[j] && words.has(s.slice(j, i));
    }
  }
  return ok[s.length];
}
// "leetcode" with [leet, code] → true; "catsandog" with [cats,dog,sand,and,cat] → false.

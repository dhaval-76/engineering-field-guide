// Fewest words in a chain begin → end, changing one letter at a time, every word in the
// list. The graph is implicit: neighbours are generated, not stored. BFS gives the fewest
// steps.
function ladderLength(begin, end, wordList) {
  const words = new Set(wordList);
  if (!words.has(end)) return 0;
  let level = [begin];
  words.delete(begin); // deleting = marking visited
  for (let length = 1; level.length > 0; length++) {
    const next = [];
    for (const word of level) {
      for (let i = 0; i < word.length; i++) {
        for (let c = 97; c <= 122; c++) {
          const candidate = word.slice(0, i) + String.fromCharCode(c) + word.slice(i + 1);
          if (!words.has(candidate)) continue; // not a word, or already seen
          if (candidate === end) return length + 1;
          words.delete(candidate);
          next.push(candidate);
        }
      }
    }
    level = next;
  }
  return 0;
}
// "hit" → "cog" with [hot,dot,dog,lot,log,cog] → 5 (hit hot dot dog cog). O(N · L · 26).

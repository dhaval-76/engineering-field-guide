// Fewest words in a chain begin → end, changing one letter at a time, every word in the
// list. The graph is implicit: neighbours are generated, not stored. BFS gives the fewest
// steps.
int ladderLength(String begin, String end, List<String> wordList) {
    Set<String> words = new HashSet<>(wordList);
    if (!words.contains(end)) return 0;
    Deque<String> queue = new ArrayDeque<>(List.of(begin));
    words.remove(begin);                           // removing = marking visited
    for (int length = 1; !queue.isEmpty(); length++) {
        for (int size = queue.size(); size > 0; size--) {   // one BFS level
            char[] w = queue.poll().toCharArray();
            for (int i = 0; i < w.length; i++) {
                char original = w[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    w[i] = c;
                    String next = new String(w);
                    if (!words.remove(next)) continue;      // not a word, or already seen
                    if (next.equals(end)) return length + 1;
                    queue.offer(next);
                }
                w[i] = original;
            }
        }
    }
    return 0;
}
// "hit" → "cog" with [hot,dot,dog,lot,log,cog] → 5 (hit hot dot dog cog). O(N · L · 26).

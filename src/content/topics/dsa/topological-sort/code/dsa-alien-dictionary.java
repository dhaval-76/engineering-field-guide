/** Words sorted in an unknown alphabet → one valid letter order, or "" if inconsistent. */
String alienOrder(String[] words) {
    Map<Character, Set<Character>> next = new HashMap<>();
    Map<Character, Integer> inDegree = new HashMap<>();
    for (String w : words) for (char c : w.toCharArray()) { next.putIfAbsent(c, new HashSet<>()); inDegree.putIfAbsent(c, 0); }

    for (int i = 0; i + 1 < words.length; i++) {
        String a = words[i], b = words[i + 1];
        if (a.length() > b.length() && a.startsWith(b)) return "";     // "abc" before "ab": invalid
        for (int j = 0; j < Math.min(a.length(), b.length()); j++) {
            char x = a.charAt(j), y = b.charAt(j);
            if (x != y) {                                               // only the FIRST difference tells us anything
                if (next.get(x).add(y)) inDegree.merge(y, 1, Integer::sum);
                break;
            }
        }
    }

    Deque<Character> ready = new ArrayDeque<>();
    inDegree.forEach((c, d) -> { if (d == 0) ready.offer(c); });
    StringBuilder order = new StringBuilder();
    while (!ready.isEmpty()) {
        char c = ready.poll();
        order.append(c);
        for (char d : next.get(c)) if (inDegree.merge(d, -1, Integer::sum) == 0) ready.offer(d);
    }
    return order.length() == inDegree.size() ? order.toString() : "";  // cycle → contradictory input
}
// ["wrt","wrf","er","ett","rftt"] → "wertf"

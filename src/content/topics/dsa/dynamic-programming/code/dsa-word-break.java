// Can s be split into a sequence of dictionary words?
// ok[i] = the prefix s[0..i) can be split. ok[i] is true if some word ends exactly at i
// and the text before that word can be split.
boolean wordBreak(String s, List<String> dictionary) {
    Set<String> words = new HashSet<>(dictionary);
    int longest = dictionary.stream().mapToInt(String::length).max().orElse(0);
    boolean[] ok = new boolean[s.length() + 1];
    ok[0] = true;                                    // the empty prefix
    for (int i = 1; i <= s.length(); i++) {
        for (int j = Math.max(0, i - longest); j < i && !ok[i]; j++) {
            ok[i] = ok[j] && words.contains(s.substring(j, i));
        }
    }
    return ok[s.length()];
}
// "leetcode" with [leet, code] → true; "catsandog" with [cats,dog,sand,and,cat] → false.
// O(n · L) substring checks for longest word length L (each check costs O(L)).

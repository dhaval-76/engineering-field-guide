// All balanced strings of n pairs. Prune instead of generating all 2^(2n) strings:
// add '(' while some are left; add ')' only while it would close an open one.
List<String> generateParenthesis(int n) {
    List<String> out = new ArrayList<>();
    generate(n, 0, 0, new StringBuilder(), out);
    return out;
}

void generate(int n, int open, int close, StringBuilder s, List<String> out) {
    if (s.length() == 2 * n) { out.add(s.toString()); return; }
    if (open < n) {
        s.append('(');
        generate(n, open + 1, close, s, out);
        s.deleteCharAt(s.length() - 1);
    }
    if (close < open) {                          // never more ')' than '(' so far
        s.append(')');
        generate(n, open, close + 1, s, out);
        s.deleteCharAt(s.length() - 1);
    }
}
// n = 3 → ((())), (()()), (())(), ()(()), ()()(). The count is the Catalan number C(n).

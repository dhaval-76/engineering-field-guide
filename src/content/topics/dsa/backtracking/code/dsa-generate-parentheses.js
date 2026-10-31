// All balanced strings of n pairs. Prune instead of generating all 2^(2n) strings:
// add '(' while some are left; add ')' only while it would close an open one.
function generateParenthesis(n) {
  const out = [];
  const generate = (s, open, close) => {
    if (s.length === 2 * n) {
      out.push(s);
      return;
    }
    if (open < n) generate(s + '(', open + 1, close);
    if (close < open) generate(s + ')', open, close + 1); // never more ')' than '('
  };
  generate('', 0, 0);
  return out;
}
// n = 3 → ((())), (()()), (())(), ()(()), ()()(). The count is the Catalan number C(n).

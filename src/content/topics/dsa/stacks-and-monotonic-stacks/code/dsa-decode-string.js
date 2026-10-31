// "3[a2[c]]" → "accaccacc". Each '[' saves the outer state; each ']' finishes a level.
function decodeString(s) {
  const stack = []; // [textBefore, count] per open bracket
  let current = '';
  let k = 0;
  for (const c of s) {
    if (c >= '0' && c <= '9') {
      k = k * 10 + Number(c); // counts can have several digits
    } else if (c === '[') {
      stack.push([current, k]);
      current = '';
      k = 0;
    } else if (c === ']') {
      const [before, count] = stack.pop();
      current = before + current.repeat(count); // back to the enclosing level
    } else {
      current += c;
    }
  }
  return current;
}
// "3[a]2[bc]" → "aaabcbc", "2[abc]3[cd]ef" → "abcabccdcdcdef". O(output length).

function isValid(s) {
  const openerFor = { ')': '(', ']': '[', '}': '{' };
  const stack = [];
  for (const c of s) {
    if (c in openerFor) {
      if (stack.pop() !== openerFor[c]) return false;   // pop() of empty → undefined
    } else stack.push(c);
  }
  return stack.length === 0;
}

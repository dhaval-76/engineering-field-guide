// Reverse Polish notation: operands first, then the operator. ["2","1","+","3","*"] =
// (2+1)*3.
function evalRPN(tokens) {
  const stack = [];
  const ops = {
    '+': (a, b) => a + b,
    '-': (a, b) => a - b,
    '*': (a, b) => a * b,
    '/': (a, b) => Math.trunc(a / b), // truncates toward zero
  };
  for (const t of tokens) {
    if (t in ops) {
      const b = stack.pop(); // order matters for - and /
      const a = stack.pop();
      stack.push(ops[t](a, b));
    } else {
      stack.push(Number(t));
    }
  }
  return stack.pop();
}
// ["4","13","5","/","+"] → 6. O(n).

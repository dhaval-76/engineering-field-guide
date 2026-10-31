// A tiny formula language: numbers, cell refs, + - * / and SUM(A1:B3). Each node: {
// eval(get), refs }.
const num = (v) => ({ eval: () => v, refs: [] });
const ref = (id) => ({ eval: (get) => get(id), refs: [id] });
const OPS = {
  '+': (a, b) => a + b,
  '-': (a, b) => a - b,
  '*': (a, b) => a * b,
  '/': (a, b) => a / b,
};
const bin = (op, l, r) => ({
  eval: (get) => OPS[op](l.eval(get), r.eval(get)),
  refs: [...new Set([...l.refs, ...r.refs])],
});
const sum = (cells) => ({ eval: (get) => cells.reduce((s, c) => s + get(c), 0), refs: cells });

// 'A1', 'B3' → A1 A2 A3 B1 B2 B3 (single-letter columns for brevity)
export function expandRange(a, b) {
  const [, c1, r1] = a.match(/^([A-Z])(\d+)$/),
    [, c2, r2] = b.match(/^([A-Z])(\d+)$/);
  const out = [];
  for (
    let c = Math.min(c1.charCodeAt(0), c2.charCodeAt(0));
    c <= Math.max(c1.charCodeAt(0), c2.charCodeAt(0));
    c++
  )
    for (let r = Math.min(+r1, +r2); r <= Math.max(+r1, +r2); r++)
      out.push(String.fromCharCode(c) + r);
  return out;
}

// Recursive descent: expr := term (('+'|'-') term)*, term := factor (('*'|'/') factor)*
export function parse(input) {
  const s = input.replace(/\s+/g, '');
  let i = 0;
  const error = () => new Error(`bad formula near position ${i}: ${s}`);
  const peek = (c) => s[i] === c;
  const expect = (c) => {
    if (!peek(c)) throw error();
    i++;
  };
  const match = (re) => {
    re.lastIndex = i;
    const m = re.exec(s);
    if (!m) throw error();
    i = re.lastIndex;
    return m[0];
  };
  const factor = () => {
    if (peek('(')) {
      i++;
      const e = expr();
      expect(')');
      return e;
    }
    if (s.startsWith('SUM(', i)) {
      i += 4;
      const a = match(/[A-Z]+\d+/y);
      expect(':');
      const b = match(/[A-Z]+\d+/y);
      expect(')');
      return sum(expandRange(a, b));
    }
    if (/[A-Z]/.test(s[i] ?? '')) return ref(match(/[A-Z]+\d+/y));
    return num(parseFloat(match(/\d+(\.\d+)?/y)));
  };
  const term = () => {
    let e = factor();
    while (peek('*') || peek('/')) e = bin(s[i++], e, factor());
    return e;
  };
  const expr = () => {
    let e = term();
    while (peek('+') || peek('-')) e = bin(s[i++], e, term());
    return e;
  };
  const e = expr();
  if (i !== s.length) throw error();
  return e;
}

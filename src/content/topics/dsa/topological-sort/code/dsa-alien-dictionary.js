function alienOrder(words) {
  const next = new Map(), inDegree = new Map();
  for (const w of words) {
    for (const c of w) {
      if (!next.has(c)) {
        next.set(c, new Set());
        inDegree.set(c, 0);
      }
    }
  }
  for (let i = 0; i + 1 < words.length; i++) {
    const a = words[i], b = words[i + 1];
    // "abc" before "ab" is impossible
    if (a.length > b.length && a.startsWith(b)) return '';
    for (let j = 0; j < Math.min(a.length, b.length); j++) {
      if (a[j] !== b[j]) {
        if (!next.get(a[j]).has(b[j])) {
          next.get(a[j]).add(b[j]);
          inDegree.set(b[j], inDegree.get(b[j]) + 1);
        }
        // only the first difference tells us anything
        break;
      }
    }
  }
  const ready = [...inDegree].filter(([, d]) => d === 0).map(([c]) => c);
  const order = [];
  for (let i = 0; i < ready.length; i++) {
    const c = ready[i];
    order.push(c);
    for (const d of next.get(c)) {
      inDegree.set(d, inDegree.get(d) - 1);
      if (inDegree.get(d) === 0) ready.push(d);
    }
  }
  // leftover letters ⇒ cycle
  return order.length === inDegree.size ? order.join('') : '';
}

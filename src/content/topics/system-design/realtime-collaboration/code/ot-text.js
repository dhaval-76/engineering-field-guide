// The same transform in JavaScript (ops: {type:'ins', pos, text} | {type:'del', pos, len})
export function transform(op, applied, opWinsTies) {
  if (op.type === 'ins') {
    if (applied.type === 'ins') {
      const shift = applied.pos < op.pos || (applied.pos === op.pos && !opWinsTies);
      return shift ? { ...op, pos: op.pos + applied.text.length } : op;
    }
    if (op.pos <= applied.pos) return op;
    if (op.pos >= applied.pos + applied.len) return { ...op, pos: op.pos - applied.len };
    return { ...op, pos: applied.pos, text: '' };   // typed inside a range someone deleted: dropped (same rule both ways)
  }
  if (applied.type === 'ins') {
    if (applied.pos <= op.pos) return { ...op, pos: op.pos + applied.text.length };
    if (applied.pos >= op.pos + op.len) return op;
    return { ...op, len: op.len + applied.text.length };
  }
  const [s, e, ds, de] = [op.pos, op.pos + op.len, applied.pos, applied.pos + applied.len];
  if (e <= ds) return op;
  if (s >= de) return { ...op, pos: s - applied.len };
  const overlap = Math.min(e, de) - Math.max(s, ds);
  return { type: 'del', pos: Math.min(s, ds), len: op.len - overlap };
}

export const apply = (doc, op) =>
  op.type === 'ins' ? doc.slice(0, op.pos) + op.text + doc.slice(op.pos)
                    : doc.slice(0, op.pos) + doc.slice(op.pos + op.len);

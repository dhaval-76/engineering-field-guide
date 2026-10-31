// Merge two edited versions of a document made of blocks (paragraphs) with stable ids.
// base/ours/theirs: Map(blockId -> text); a missing id means absent or deleted.
export function merge(base, ours, theirs) {
  const merged = new Map();
  const conflicts = [];
  const ids = new Set([...base.keys(), ...ours.keys(), ...theirs.keys()]);
  for (const id of ids) {
    const b = base.get(id), o = ours.get(id), t = theirs.get(id);
    if (o === t) { if (o !== undefined) merged.set(id, o); }           // same on both sides
    else if (o === b) { if (t !== undefined) merged.set(id, t); }      // only theirs changed
    else if (t === b) { if (o !== undefined) merged.set(id, o); }      // only ours changed
    else conflicts.push({ blockId: id, base: b, ours: o, theirs: t }); // both changed differently
  }
  return { blocks: merged, conflicts }; // real implementations also merge block ORDER
}

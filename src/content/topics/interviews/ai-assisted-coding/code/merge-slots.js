// Merge half-open [start, end) slots that overlap or touch. Input may be unsorted; not
// mutated.
export function mergeSlots(slots) {
  const sorted = slots.map(([s, e]) => [s, e]).sort((a, b) => a[0] - b[0]);
  const out = [];
  for (const slot of sorted) {
    const last = out.at(-1);
    if (last && slot[0] <= last[1]) last[1] = Math.max(last[1], slot[1]);
    else out.push(slot);
  }
  return out;
}

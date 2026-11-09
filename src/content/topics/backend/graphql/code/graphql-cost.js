/** The number of fields the server would resolve if every list came back full. */
function analyze(selections) {
  let fields = 0;
  let depth = 0;
  let nestedLists = 0;
  for (const s of selections) {
    const below = analyze(s.children ?? []);
    fields += 1 + Math.max(1, s.listSize ?? 0) * below.fields;
    depth = Math.max(depth, 1 + below.depth);
    nestedLists = Math.max(nestedLists, (s.listSize ? 1 : 0) + below.nestedLists);
  }
  return { fields, depth, nestedLists };
}

/** Null means allowed. Runs before any resolver does. */
function rejectionReason(query, { maxFields, maxDepth, maxNestedLists }) {
  const cost = analyze(query);
  if (cost.depth > maxDepth) return `too deep: ${cost.depth}`;
  if (cost.nestedLists > maxNestedLists) return `too many nested lists: ${cost.nestedLists}`;
  if (cost.fields > maxFields) return `too expensive: ${cost.fields} fields`;
  return null;
}

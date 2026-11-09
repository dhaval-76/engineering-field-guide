/**
 * Renders a prompt with the most stable parts first, so successive requests share the longest
 * possible prefix. Anything that changes per request goes last.
 */
function assemble({ instructions, tools, history, retrieved, question, today }) {
  // Sorted: the same tools in a different order would be a different prefix.
  const toolBlock = [...tools]
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    .map((t) => `${t.name}: ${t.schema}`)
    .join("\n");
  return [
    { role: "system", text: `${instructions}\n\nTools:\n${toolBlock}` },
    ...history,
    // Today's date lives here, at the end. In the system prompt it would break the cache daily.
    { role: "user", text: `Date: ${today}\n\n${retrieved}\n\n${question}` },
  ];
}

/** For a test: how many leading messages two rendered prompts share. */
function sharedPrefix(a, b) {
  let n = 0;
  while (n < a.length && n < b.length && JSON.stringify(a[n]) === JSON.stringify(b[n])) n++;
  return n;
}

/** Lists what would break existing clients. Run it in CI against the published contract. */
function breakingChanges(before, after, direction) {
  const problems = [];
  for (const [name, old] of Object.entries(before)) {
    const now = after[name];
    if (!now) problems.push(`removed: ${name}`);
    else if (now.type !== old.type) problems.push(`type changed: ${name}`);
    else if (direction === "request" && now.required && !old.required) {
      problems.push(`now required: ${name}`);
    } else if (direction === "response" && old.required && !now.required) {
      problems.push(`no longer guaranteed: ${name}`);
    }
  }
  if (direction === "request") {
    for (const [name, spec] of Object.entries(after)) {
      if (spec.required && !(name in before)) problems.push(`new required field: ${name}`);
    }
  }
  return problems;
}

/**
 * The heart of every declarative tool (Terraform plan, Kubernetes controllers): compare what
 * the code declares with what exists, and list only the differences. Running it again on an
 * already-matching world produces nothing to do.
 * Both arguments map a resource name to its settings, e.g. { "bucket:logs": { versioning: "on" } }.
 */
function plan(desired, actual) {
  const same = (a, b) => JSON.stringify(Object.entries(a).sort()) === JSON.stringify(Object.entries(b).sort());
  const actions = [];
  for (const name of Object.keys(desired).sort()) {
    if (!(name in actual)) actions.push({ kind: 'create', name });
    else if (!same(desired[name], actual[name])) actions.push({ kind: 'update', name });
  }
  for (const name of Object.keys(actual).sort()) {
    // exists but no longer declared, or created by hand and not in code: drift
    if (!(name in desired)) actions.push({ kind: 'delete', name });
  }
  return actions;
}

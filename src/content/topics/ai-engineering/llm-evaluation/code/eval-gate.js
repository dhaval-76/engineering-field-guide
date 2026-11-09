/** Runs every case once and records which ones fail. */
async function run(cases, model) {
  const failed = new Set();
  for (const c of cases) {
    if (!c.passes(await model(c.input))) failed.add(c.id);
  }
  return { total: cases.length, failed };
}

/** 95% Wilson interval for a pass rate: how far the true rate may be from what you measured. */
function wilson(passed, total) {
  const z = 1.96;
  const p = passed / total;
  const centre = (p + (z * z) / (2 * total)) / (1 + (z * z) / total);
  const margin =
    (z * Math.sqrt((p * (1 - p)) / total + (z * z) / (4 * total * total))) / (1 + (z * z) / total);
  return [centre - margin, centre + margin];
}

/** Two-sided sign test on the cases that changed; the cases that did not change carry no signal. */
function signTest(worse, better) {
  const n = worse + better;
  if (n === 0) return 1;
  let tail = 0;
  let term = 0.5 ** n; // term = C(n, i) * 0.5^n
  for (let i = 0; i <= Math.min(worse, better); i++) {
    tail += term;
    term = (term * (n - i)) / (i + 1);
  }
  return Math.min(1, 2 * tail);
}

/** Compares the same cases under two versions. Matching cases makes small changes detectable. */
function compare(baseline, candidate) {
  const newFailures = [...candidate.failed].filter((id) => !baseline.failed.has(id));
  const fixed = [...baseline.failed].filter((id) => !candidate.failed.has(id));
  return { newFailures, fixed, pValue: signTest(newFailures.length, fixed.length) };
}

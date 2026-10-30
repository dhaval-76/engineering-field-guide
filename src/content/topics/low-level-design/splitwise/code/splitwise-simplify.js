// netBalances: { alice: 75000, bob: 8333, carol: -11667, dave: -71666 }  (paise)
export function simplify(netBalances) {
  const creditors = [], debtors = [];
  for (const [person, bal] of Object.entries(netBalances)) {
    if (bal > 0) creditors.push([person, bal]);
    else if (bal < 0) debtors.push([person, -bal]);
  }
  const byAmountDesc = (a, b) => b[1] - a[1];
  const plan = [];
  while (creditors.length && debtors.length) {
    creditors.sort(byAmountDesc); // fine for small groups; use a heap for large ones
    debtors.sort(byAmountDesc);
    const [c, credit] = creditors[0];
    const [d, debt] = debtors[0];
    const amount = Math.min(credit, debt);
    plan.push({ from: d, to: c, amount });
    creditors[0][1] -= amount;
    debtors[0][1] -= amount;
    if (creditors[0][1] === 0) creditors.shift();
    if (debtors[0][1] === 0) debtors.shift();
  }
  return plan;
}

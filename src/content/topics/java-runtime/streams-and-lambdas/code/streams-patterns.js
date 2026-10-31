// Arrays are eager (each step builds a new array); fine for typical sizes.
const top3 = Object.entries(
  orders
    .filter((o) => o.status !== 'CANCELLED')
    .reduce((acc, o) => ({ ...acc, [o.customerId]: (acc[o.customerId] ?? 0) + o.totalPaise }), {}),
)
  .sort(([, a], [, b]) => b - a)
  .slice(0, 3)
  .map(([id]) => id);

// Grouping (ES2024): Object.groupBy / Map.groupBy
const byDept = Object.groupBy(employees, (e) => e.dept);

// Lazy pipelines (stop early) with iterator helpers in modern runtimes: const first =
// Iterator.from(strings).filter((s) => s.startsWith('A')).take(1).toArray();

function leastInterval(tasks, n) {
  const count = new Map();
  for (const t of tasks) count.set(t, (count.get(t) ?? 0) + 1);
  const maxCount = Math.max(...count.values());
  const tied = [...count.values()].filter((c) => c === maxCount).length;
  return Math.max(tasks.length, (maxCount - 1) * (n + 1) + tied);
}

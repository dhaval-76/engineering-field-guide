function findOrder(n, prerequisites) {
  const next = Array.from({ length: n }, () => []);
  const inDegree = new Array(n).fill(0);
  for (const [course, pre] of prerequisites) {
    next[pre].push(course);
    inDegree[course]++;
  }
  const ready = [];
  for (let i = 0; i < n; i++) if (inDegree[i] === 0) ready.push(i);
  const order = [];
  for (let head = 0; head < ready.length; head++) {
    const course = ready[head];
    order.push(course);
    for (const c of next[course]) if (--inDegree[c] === 0) ready.push(c);
  }
  return order.length === n ? order : [];
}

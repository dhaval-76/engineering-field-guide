// Tasks 0..n-1 with durations; deps[i] = [before, after]. Unlimited workers run
// independent tasks in parallel. Minimum total time = the longest (critical) path, or -1
// on a cycle.
function minimumTime(n, deps, duration) {
  const next = Array.from({ length: n }, () => []);
  const inDegree = new Array(n).fill(0);
  for (const [a, b] of deps) {
    next[a].push(b);
    inDegree[b]++;
  }
  const start = new Array(n).fill(0); // earliest start = latest prerequisite finish
  const ready = [];
  for (let i = 0; i < n; i++) if (inDegree[i] === 0) ready.push(i);
  let done = 0;
  let total = 0;
  for (let head = 0; head < ready.length; head++) {
    const u = ready[head];
    done++;
    const finish = start[u] + duration[u];
    total = Math.max(total, finish);
    for (const v of next[u]) {
      start[v] = Math.max(start[v], finish); // v waits for its slowest prerequisite
      if (--inDegree[v] === 0) ready.push(v);
    }
  }
  return done === n ? total : -1;
}
// durations [1,2,3,4,5], tasks 0–3 all before task 4 → 4 + 5 = 9. O(V + E).

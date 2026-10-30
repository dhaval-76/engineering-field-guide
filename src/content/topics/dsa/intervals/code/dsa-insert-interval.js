function insert(intervals, [s0, e0]) {
  const out = [];
  let i = 0, s = s0, e = e0;
  while (i < intervals.length && intervals[i][1] < s) out.push(intervals[i++]);
  while (i < intervals.length && intervals[i][0] <= e) {
    s = Math.min(s, intervals[i][0]);
    e = Math.max(e, intervals[i][1]);
    i++;
  }
  out.push([s, e]);
  return out.concat(intervals.slice(i));
}

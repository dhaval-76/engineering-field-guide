function eraseOverlapIntervals(intervals) {
  const sorted = [...intervals].sort((a, b) => a[1] - b[1]);
  let kept = 0, lastEnd = -Infinity;
  for (const [s, e] of sorted) if (s >= lastEnd) { kept++; lastEnd = e; }
  return intervals.length - kept;
}

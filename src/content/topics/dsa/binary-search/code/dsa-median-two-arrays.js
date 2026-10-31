// Median of two sorted arrays in O(log(min(m, n))): binary search for a split of the
// shorter array such that everything on the left of both splits ≤ everything on the
// right.
function findMedianSortedArrays(a, b) {
  if (a.length > b.length) return findMedianSortedArrays(b, a);
  const m = a.length;
  const n = b.length;
  const half = Math.floor((m + n + 1) / 2);
  let lo = 0;
  let hi = m;
  while (lo <= hi) {
    const i = Math.floor((lo + hi) / 2); // elements taken from a
    const j = half - i; // elements taken from b
    const aLeft = i === 0 ? -Infinity : a[i - 1];
    const aRight = i === m ? Infinity : a[i];
    const bLeft = j === 0 ? -Infinity : b[j - 1];
    const bRight = j === n ? Infinity : b[j];
    if (aLeft <= bRight && bLeft <= aRight) {
      const leftMax = Math.max(aLeft, bLeft);
      return (m + n) % 2 === 1 ? leftMax : (leftMax + Math.min(aRight, bRight)) / 2;
    }
    if (aLeft > bRight) hi = i - 1; // took too many from a
    else lo = i + 1; // took too few from a
  }
  throw new Error('inputs must be sorted');
}
// [1,3] and [2] → 2; [1,2] and [3,4] → 2.5

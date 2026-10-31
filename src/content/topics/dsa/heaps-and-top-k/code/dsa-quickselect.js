// Kth largest in O(n) average, in place: partition like quicksort, recurse into one side
// only.
function findKthLargest(nums, k) {
  const target = nums.length - k; // index of the answer in sorted order
  let lo = 0;
  let hi = nums.length - 1;
  for (;;) {
    const pivotIndex = lo + Math.floor(Math.random() * (hi - lo + 1));
    const p = partition(nums, lo, hi, pivotIndex);
    if (p === target) return nums[p];
    if (p < target) lo = p + 1;
    else hi = p - 1;
  }
}

// Lomuto partition: smaller values left of the pivot's final index, which is returned.
function partition(a, lo, hi, pivotIndex) {
  const pivot = a[pivotIndex];
  [a[pivotIndex], a[hi]] = [a[hi], a[pivotIndex]];
  let store = lo;
  for (let i = lo; i < hi; i++) {
    if (a[i] < pivot) {
      [a[i], a[store]] = [a[store], a[i]];
      store++;
    }
  }
  [a[store], a[hi]] = [a[hi], a[store]];
  return store;
}
// [3,2,1,5,6,4], k = 2 → 5. Random pivots make the O(n²) worst case vanishingly unlikely.

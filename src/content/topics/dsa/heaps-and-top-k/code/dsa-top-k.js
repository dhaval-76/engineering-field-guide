// uses the Heap class from the previous example
function findKthLargest(nums, k) {
  const heap = new Heap();                     // min-heap
  for (const x of nums) {
    heap.push(x);
    if (heap.size > k) heap.pop();
  }
  return heap.peek();
}

function topKFrequent(nums, k) {
  const freq = new Map();
  for (const x of nums) freq.set(x, (freq.get(x) ?? 0) + 1);
  const heap = new Heap((a, b) => a[1] < b[1]);            // min-heap by count
  for (const entry of freq) {
    heap.push(entry);
    if (heap.size > k) heap.pop();
  }
  const out = [];
  while (heap.size) out.push(heap.pop()[0]);
  return out;
}

// Kth largest in O(n) average, in place: partition like quicksort, recurse into one side
// only.
int findKthLargest(int[] nums, int k) {
    int target = nums.length - k;                 // index of the answer in sorted order
    int lo = 0, hi = nums.length - 1;
    Random random = new Random();
    while (true) {
        int p = partition(nums, lo, hi, lo + random.nextInt(hi - lo + 1));
        if (p == target) return nums[p];
        if (p < target) lo = p + 1; else hi = p - 1;
    }
}

// Lomuto partition: smaller values left of the pivot's final index, which is returned.
int partition(int[] a, int lo, int hi, int pivotIndex) {
    int pivot = a[pivotIndex];
    swap(a, pivotIndex, hi);
    int store = lo;
    for (int i = lo; i < hi; i++) if (a[i] < pivot) swap(a, i, store++);
    swap(a, store, hi);
    return store;
}

void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
// [3,2,1,5,6,4], k = 2 → 5. Random pivots make the O(n²) worst case vanishingly unlikely.

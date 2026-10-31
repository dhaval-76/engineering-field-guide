// Median of two sorted arrays in O(log(min(m, n))): binary search for a split of the
// shorter array such that everything on the left of both splits ≤ everything on the
// right.
double findMedianSortedArrays(int[] a, int[] b) {
    if (a.length > b.length) return findMedianSortedArrays(b, a);
    int m = a.length, n = b.length, half = (m + n + 1) / 2;
    int lo = 0, hi = m;
    while (lo <= hi) {
        int i = (lo + hi) / 2;           // elements taken from a
        int j = half - i;                // elements taken from b
        int aLeft = i == 0 ? Integer.MIN_VALUE : a[i - 1];
        int aRight = i == m ? Integer.MAX_VALUE : a[i];
        int bLeft = j == 0 ? Integer.MIN_VALUE : b[j - 1];
        int bRight = j == n ? Integer.MAX_VALUE : b[j];
        if (aLeft <= bRight && bLeft <= aRight) {          // a valid split
            int leftMax = Math.max(aLeft, bLeft);
            if ((m + n) % 2 == 1) return leftMax;
            return (leftMax + (double) Math.min(aRight, bRight)) / 2;
        }
        if (aLeft > bRight) hi = i - 1;  // took too many from a
        else lo = i + 1;                 // took too few from a
    }
    throw new IllegalArgumentException("inputs must be sorted");
}
// [1,3] and [2] → 2.0; [1,2] and [3,4] → 2.5

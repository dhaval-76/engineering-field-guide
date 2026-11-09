List<Integer> findClosest(int[] arr, int k, int x) {
    // Binary search the left edge of a window of size k.
    int lo = 0, hi = arr.length - k;
    while (lo < hi) {
        int mid = (lo + hi) >>> 1;
        if (x - arr[mid] > arr[mid + k] - x) lo = mid + 1;
        else hi = mid;
    }
    List<Integer> out = new ArrayList<>();
    for (int i = lo; i < lo + k; i++) out.add(arr[i]);
    return out;
}

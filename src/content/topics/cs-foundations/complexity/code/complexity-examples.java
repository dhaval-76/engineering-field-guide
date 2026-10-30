// O(1): no dependence on n
int first(int[] a) { return a[0]; }

// O(n): one pass
long sum(int[] a) { long s = 0; for (int x : a) s += x; return s; }

// O(n²): every pair (the inner loop shrinks, but n(n−1)/2 is still quadratic)
boolean hasDuplicateBrute(int[] a) {
    for (int i = 0; i < a.length; i++)
        for (int j = i + 1; j < a.length; j++)
            if (a[i] == a[j]) return true;
    return false;
}

// O(n) time, O(n) space: trade memory for speed
boolean hasDuplicate(int[] a) {
    Set<Integer> seen = new HashSet<>();
    for (int x : a) if (!seen.add(x)) return true;
    return false;
}

// O(log n): the remaining range halves every step
int countHalvings(int n) { int steps = 0; while (n > 1) { n /= 2; steps++; } return steps; }

// O(n + m): two different inputs
int[] mergeSorted(int[] a, int[] b) {
    int[] out = new int[a.length + b.length];
    int i = 0, j = 0, k = 0;
    while (i < a.length && j < b.length) out[k++] = a[i] <= b[j] ? a[i++] : b[j++];
    while (i < a.length) out[k++] = a[i++];
    while (j < b.length) out[k++] = b[j++];
    return out;
}

// Hidden O(n²): list.contains is O(n), called n times
List<Integer> uniqueSlow(List<Integer> xs) {
    List<Integer> out = new ArrayList<>();
    for (int x : xs) if (!out.contains(x)) out.add(x);   // use a LinkedHashSet → O(n)
    return out;
}

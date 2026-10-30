class MinHeap {
    private int[] a = new int[16];
    private int size;

    void push(int x) {
        if (size == a.length) a = Arrays.copyOf(a, size * 2);
        a[size] = x;
        int i = size++;
        while (i > 0 && a[(i - 1) / 2] > a[i]) {                 // sift up
            swap(i, (i - 1) / 2);
            i = (i - 1) / 2;
        }
    }

    int pop() {
        if (size == 0) throw new NoSuchElementException();
        int top = a[0];
        a[0] = a[--size];
        int i = 0;
        while (true) {                                           // sift down
            int l = 2 * i + 1, r = l + 1, smallest = i;
            if (l < size && a[l] < a[smallest]) smallest = l;
            if (r < size && a[r] < a[smallest]) smallest = r;
            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
        return top;
    }

    int peek() { return a[0]; }
    int size() { return size; }
    private void swap(int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
}

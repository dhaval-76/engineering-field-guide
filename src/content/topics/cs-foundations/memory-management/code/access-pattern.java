/** Reads every element once, in order or in a shuffled order. Same work, same result. */
double nanosPerRead(int[] data, int[] order) {
    long sum = 0, start = System.nanoTime();
    for (int i : order) sum += data[i];
    double perRead = (System.nanoTime() - start) / (double) order.length;
    if (sum == 42) System.out.println(); // keeps the JIT from removing the loop
    return perRead;
}

int[][] inOrderAndShuffled(int n) {
    int[] inOrder = new int[n], shuffled = new int[n];
    for (int i = 0; i < n; i++) inOrder[i] = shuffled[i] = i;
    Random random = new Random(1);
    for (int i = n - 1; i > 0; i--) { // Fisher-Yates shuffle
        int j = random.nextInt(i + 1), t = shuffled[i];
        shuffled[i] = shuffled[j];
        shuffled[j] = t;
    }
    return new int[][] {inOrder, shuffled};
}

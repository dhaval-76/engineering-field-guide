int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
    int n = profits.length;
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingInt(i -> capital[i]));
    PriorityQueue<Integer> affordable = new PriorityQueue<>(Comparator.reverseOrder());
    int next = 0;
    for (int done = 0; done < k; done++) {
        while (next < n && capital[order[next]] <= w) affordable.add(profits[order[next++]]);
        if (affordable.isEmpty()) break;
        w += affordable.poll();
    }
    return w;
}

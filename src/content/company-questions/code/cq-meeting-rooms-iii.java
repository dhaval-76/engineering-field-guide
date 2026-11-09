int mostBooked(int n, int[][] meetings) {
    Arrays.sort(meetings, Comparator.comparingInt(m -> m[0]));
    PriorityQueue<Integer> free = new PriorityQueue<>();
    for (int i = 0; i < n; i++) free.add(i);
    // {endTime, room}, earliest end first, then lowest room
    PriorityQueue<long[]> busy = new PriorityQueue<>(
            (a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
    int[] count = new int[n];
    for (int[] m : meetings) {
        while (!busy.isEmpty() && busy.peek()[0] <= m[0]) free.add((int) busy.poll()[1]);
        long start = m[0];
        int room;
        if (!free.isEmpty()) room = free.poll();
        else { // delay until the first room frees up
            long[] first = busy.poll();
            start = first[0];
            room = (int) first[1];
        }
        busy.add(new long[] {start + m[1] - m[0], room});
        count[room]++;
    }
    int best = 0;
    for (int i = 1; i < n; i++) if (count[i] > count[best]) best = i;
    return best;
}

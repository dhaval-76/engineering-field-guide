/** Minimum rooms so no two meetings in one room overlap. A room freed at t can host a meeting starting at t. */
int minMeetingRooms(int[][] meetings) {
    int[][] sorted = meetings.clone();
    Arrays.sort(sorted, Comparator.comparingInt(m -> m[0]));
    PriorityQueue<Integer> ends = new PriorityQueue<>();          // end times of rooms in use
    for (int[] m : sorted) {
        if (!ends.isEmpty() && ends.peek() <= m[0]) ends.poll();  // reuse the room that frees earliest
        ends.offer(m[1]);
    }
    return ends.size();
}

/** Same answer with a sweep line: +1 at each start, −1 at each end, ends first on ties. */
int minMeetingRoomsSweep(int[][] meetings) {
    int[] starts = Arrays.stream(meetings).mapToInt(m -> m[0]).sorted().toArray();
    int[] ends = Arrays.stream(meetings).mapToInt(m -> m[1]).sorted().toArray();
    int rooms = 0, best = 0, e = 0;
    for (int s : starts) {
        while (e < ends.length && ends[e] <= s) { e++; rooms--; }
        best = Math.max(best, ++rooms);
    }
    return best;
}
// [[0,30],[5,10],[15,20]] → 2

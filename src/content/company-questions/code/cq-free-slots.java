List<int[]> freeSlots(int dayStart, int dayEnd, int[][] meetings) {
    Arrays.sort(meetings, Comparator.comparingInt(m -> m[0]));
    List<int[]> free = new ArrayList<>();
    int cursor = dayStart;
    for (int[] m : meetings) {
        if (m[0] > cursor) free.add(new int[] {cursor, Math.min(m[0], dayEnd)});
        cursor = Math.max(cursor, m[1]);
        if (cursor >= dayEnd) return free;
    }
    if (cursor < dayEnd) free.add(new int[] {cursor, dayEnd});
    return free;
}

/** Minimum slots to run all tasks when identical tasks need n idle-or-other slots between them. */
int leastInterval(char[] tasks, int n) {
    int[] count = new int[26];
    for (char t : tasks) count[t - 'A']++;
    int maxCount = Arrays.stream(count).max().getAsInt();
    int tiedForMax = (int) Arrays.stream(count).filter(c -> c == maxCount).count();
    int framed = (maxCount - 1) * (n + 1) + tiedForMax;   // frame built around the most frequent task
    return Math.max(tasks.length, framed);                 // enough other tasks → no idle needed
}
// ["A","A","A","B","B","B"], n = 2 → 8 (A B _ A B _ A B)

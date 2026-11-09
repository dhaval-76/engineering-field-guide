/** logs[i] = {employee, leaveTime}; each task starts when the previous one ended. */
int hardestWorker(int[][] logs) {
    int start = 0, longest = 0, employee = Integer.MAX_VALUE;
    for (int[] log : logs) {
        int length = log[1] - start;
        if (length > longest || (length == longest && log[0] < employee)) {
            longest = length;
            employee = log[0];
        }
        start = log[1];
    }
    return employee;
}

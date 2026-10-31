/** Index of the station from which you can complete the circuit, or -1. */
int canCompleteCircuit(int[] gas, int[] cost) {
    int total = 0, tank = 0, start = 0;
    for (int i = 0; i < gas.length; i++) {
        int gain = gas[i] - cost[i];
        total += gain;
        tank += gain;
        // can't reach i + 1 from `start` — nor from any station in between
        if (tank < 0) {
            start = i + 1;
            tank = 0;
        }
    }
    return total >= 0 ? start : -1;   // enough gas overall ⇒ the last reset point works
}

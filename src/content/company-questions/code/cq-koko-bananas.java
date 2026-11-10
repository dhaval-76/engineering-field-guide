int minEatingSpeed(int[] piles, int hours) {
    int lo = 1, hi = Arrays.stream(piles).max().getAsInt();
    while (lo < hi) { // smallest speed that finishes in time
        int speed = lo + (hi - lo) / 2;
        long needed = 0;
        for (int p : piles) needed += (p + speed - 1) / speed; // ceil(p / speed)
        if (needed <= hours) hi = speed;
        else lo = speed + 1;
    }
    return lo;
}

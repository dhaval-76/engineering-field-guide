/** Returns the index of the free seat furthest from anyone ('O' = taken, 'U' = free). */
int bestSeat(char[] seats) {
    int last = -1, best = -1, bestDistance = 0;
    for (int i = 0; i < seats.length; i++) {
        if (seats[i] != 'O') continue;
        int seat = last < 0 ? 0 : (last + i) / 2;
        int distance = last < 0 ? i : seat - last;
        if (distance > bestDistance) {
            bestDistance = distance;
            best = seat;
        }
        last = i;
    }
    if (last >= 0 && seats.length - 1 - last > bestDistance) best = seats.length - 1;
    return best;
}

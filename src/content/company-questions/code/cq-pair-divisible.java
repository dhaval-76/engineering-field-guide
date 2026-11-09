/** Returns indices of two songs whose total is divisible by 120, or an empty array. */
int[] pairDivisibleBy120(int[] durations) {
    Map<Integer, Integer> indexByRemainder = new HashMap<>();
    for (int i = 0; i < durations.length; i++) {
        int rem = durations[i] % 120;
        Integer partner = indexByRemainder.get((120 - rem) % 120);
        if (partner != null) return new int[] {partner, i};
        indexByRemainder.put(rem, i);
    }
    return new int[0];
}

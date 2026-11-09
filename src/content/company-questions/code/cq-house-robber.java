int rob(int[] nums) {
    int skip = 0, take = 0; // best without and with robbing the previous house
    for (int money : nums) {
        int newTake = skip + money;
        skip = Math.max(skip, take);
        take = newTake;
    }
    return Math.max(skip, take);
}

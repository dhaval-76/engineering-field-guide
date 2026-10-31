boolean canJump(int[] nums) {
    int farthest = 0;
    for (int i = 0; i < nums.length; i++) {
        if (i > farthest) return false;                     // i is unreachable
        farthest = Math.max(farthest, i + nums[i]);
    }
    return true;
}

/**
 * Minimum jumps to reach the last index (assumed reachable): BFS by "levels" of reach.
 */
int minJumps(int[] nums) {
    int jumps = 0, currentEnd = 0, farthest = 0;
    for (int i = 0; i < nums.length - 1; i++) {
        farthest = Math.max(farthest, i + nums[i]);
        // used up this jump's range: must jump again
        if (i == currentEnd) {
            jumps++;
            currentEnd = farthest;
        }
    }
    return jumps;
}
// [2,3,1,1,4] → true, 2 jumps

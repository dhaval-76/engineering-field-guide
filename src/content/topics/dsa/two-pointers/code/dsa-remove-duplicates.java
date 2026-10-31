int removeDuplicates(int[] sorted) {
    if (sorted.length == 0) return 0;
    // sorted[0..write) is the unique prefix
    int write = 1;
    for (int read = 1; read < sorted.length; read++)
        if (sorted[read] != sorted[write - 1]) sorted[write++] = sorted[read];
    return write;
}
// [0,0,1,1,1,2,2,3,3,4] → returns 5, array starts [0,1,2,3,4,…]

class RandomizedSet {
    // value → position in `values`
    private final Map<Integer, Integer> indexOf = new HashMap<>();
    private final List<Integer> values = new ArrayList<>();
    private final Random random = new Random();

    boolean insert(int val) {
        if (indexOf.containsKey(val)) return false;
        indexOf.put(val, values.size());
        values.add(val);
        return true;
    }

    boolean remove(int val) {
        Integer i = indexOf.remove(val);
        if (i == null) return false;
        int last = values.remove(values.size() - 1);       // O(1): remove from the end
        // the removed value wasn't the last one:
        if (i < values.size()) {
            // move the last value into its slot
            values.set(i, last);
            indexOf.put(last, i);                          // and record its new position
        }
        return true;
    }

    // uniform, O(1)
    int getRandom() { return values.get(random.nextInt(values.size())); }
}

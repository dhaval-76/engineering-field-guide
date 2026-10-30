class RandomizedSet {
    private final Map<Integer, Integer> indexOf = new HashMap<>();   // value → position in `values`
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
        if (i < values.size()) {                           // the removed value wasn't the last one:
            values.set(i, last);                           // move the last value into its slot
            indexOf.put(last, i);                          // and record its new position
        }
        return true;
    }

    int getRandom() { return values.get(random.nextInt(values.size())); }   // uniform, O(1)
}

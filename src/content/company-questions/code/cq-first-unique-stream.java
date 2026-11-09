class FirstUnique {
    private final Set<Integer> unique = new LinkedHashSet<>(); // keeps arrival order
    private final Set<Integer> repeated = new HashSet<>();

    void add(int value) {
        if (repeated.contains(value)) return;
        if (!unique.add(value)) { // second sighting
            unique.remove(value);
            repeated.add(value);
        }
    }

    int first() {
        return unique.isEmpty() ? -1 : unique.iterator().next();
    }
}

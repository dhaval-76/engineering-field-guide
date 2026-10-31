List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));

// ✗ ConcurrentModificationException: the for-each iterator detects the structural change
for (Integer n : nums) if (n % 2 == 0) nums.remove(n);

// ✓ Iterator.remove keeps the iterator's bookkeeping consistent
for (Iterator<Integer> it = nums.iterator(); it.hasNext(); ) if (it.next() % 2 == 0) it.remove();

// ✓ Best: removeIf — one pass, compacts once (O(n) instead of O(n²) shifting for
// ArrayList)
nums.removeIf(n -> n % 2 == 0);

// Immutable vs unmodifiable:
// read-only VIEW: changes to source show through
List<String> view  = Collections.unmodifiableList(source);
List<String> copy  = List.copyOf(source);                      // immutable snapshot
// immutable; rejects nulls; throws on add
List<String> fixed = List.of("a", "b");

// Pick by the operations you need — the interface first, then the implementation.
// ordered, indexed, duplicates OK
List<Order> history        = new ArrayList<>();
Set<String> seenIds        = new HashSet<>();            // membership in O(1)
Set<String> sortedTags     = new TreeSet<>();            // sorted, range views
Map<String, User> byEmail  = new HashMap<>();            // key → value in O(1)
// sorted keys: floor/ceiling/subMap
NavigableMap<Instant, Event> timeline = new TreeMap<>();
// stack or queue, O(1) at both ends
Deque<Task> work           = new ArrayDeque<>();
// smallest first
PriorityQueue<Job> byDeadline = new PriorityQueue<>(Comparator.comparing(Job::deadline));

// Range query that a HashMap can't do:
SortedMap<Instant, Event> lastHour = timeline.subMap(now.minus(Duration.ofHours(1)), now);
Instant justBefore = timeline.floorKey(now);

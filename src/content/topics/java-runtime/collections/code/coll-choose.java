// Pick by the operations you need — the interface first, then the implementation.
List<Order> history        = new ArrayList<>();          // ordered, indexed, duplicates OK
Set<String> seenIds        = new HashSet<>();            // membership in O(1)
Set<String> sortedTags     = new TreeSet<>();            // sorted, range views
Map<String, User> byEmail  = new HashMap<>();            // key → value in O(1)
NavigableMap<Instant, Event> timeline = new TreeMap<>(); // sorted keys: floor/ceiling/subMap
Deque<Task> work           = new ArrayDeque<>();         // stack or queue, O(1) at both ends
PriorityQueue<Job> byDeadline = new PriorityQueue<>(Comparator.comparing(Job::deadline));   // smallest first

// Range query that a HashMap can't do:
SortedMap<Instant, Event> lastHour = timeline.subMap(now.minus(Duration.ofHours(1)), now);
Instant justBefore = timeline.floorKey(now);

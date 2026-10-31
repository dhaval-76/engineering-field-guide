// Bookings arrive one at a time as half-open [start, end). Reject one that overlaps any
// booking. A TreeMap (start → end) finds the only two bookings that could clash in O(log
// n).
class MyCalendar {
    private final TreeMap<Integer, Integer> booked = new TreeMap<>();

    boolean book(int start, int end) {
        // starts at or before
        Map.Entry<Integer, Integer> before = booked.floorEntry(start);
        // starts at or after
        Map.Entry<Integer, Integer> after = booked.ceilingEntry(start);
        if (before != null && before.getValue() > start) return false;   // still running
        // starts too soon
        if (after != null && after.getKey() < end) return false;
        booked.put(start, end);
        return true;
    }
}
// book(10,20) true, book(15,25) false, book(20,30) true (touching is fine). O(log n)
// each.

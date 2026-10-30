/** Half-open interval [start, end): a meeting ending at 10:00 doesn't clash with one starting at 10:00. */
record TimeSlot(Instant start, Instant end) {
    TimeSlot {
        if (!start.isBefore(end)) throw new IllegalArgumentException("start must be before end");
    }
    boolean overlaps(TimeSlot o) { return start.isBefore(o.end) && o.start.isBefore(end); }
}

record Booking(String id, String roomId, TimeSlot slot, String organizer, Set<String> attendees, String seriesId) {}

final class Room {
    final String id;
    final int capacity;
    final Set<Amenity> amenities;
    // Bookings keyed by start time — non-overlapping by construction, so neighbours are all we need to check.
    private final TreeMap<Instant, Booking> byStart = new TreeMap<>();

    Room(String id, int capacity, Set<Amenity> amenities) { this.id = id; this.capacity = capacity; this.amenities = amenities; }

    /** O(log n): only the booking that starts just before, and the one that starts just after, can overlap. */
    synchronized boolean isFree(TimeSlot s) {
        var before = byStart.floorEntry(s.start());
        if (before != null && before.getValue().slot().overlaps(s)) return false;
        var after = byStart.ceilingEntry(s.start());
        return after == null || !after.getValue().slot().overlaps(s);
    }

    synchronized Booking book(TimeSlot s, String organizer, Set<String> attendees, String seriesId) {
        if (!isFree(s)) throw new SlotTakenException(id, s);           // check and insert under one lock
        Booking b = new Booking(UUID.randomUUID().toString(), id, s, organizer, Set.copyOf(attendees), seriesId);
        byStart.put(s.start(), b);
        return b;
    }

    synchronized void cancel(Booking b) { byStart.remove(b.slot().start(), b); }

    synchronized List<Booking> between(Instant from, Instant to) {
        return List.copyOf(byStart.subMap(from, true, to, false).values());
    }
}

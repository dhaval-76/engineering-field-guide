final class RoomFinder {
    private final List<Room> rooms;
    RoomFinder(List<Room> rooms) { this.rooms = List.copyOf(rooms); }

    /** Smallest room that fits — keeps big rooms free for big meetings. */
    List<Room> candidates(TimeSlot s, int people, Set<Amenity> needs) {
        return rooms.stream()
                .filter(r -> r.capacity >= people && r.amenities.containsAll(needs) && r.isFree(s))
                .sorted(Comparator.comparingInt((Room r) -> r.capacity))
                .toList();
    }

    /** Try candidates in order; another thread may book one between the search and our attempt. */
    Booking bookBest(TimeSlot s, int people, Set<Amenity> needs, String organizer, Set<String> attendees) {
        for (Room r : candidates(s, people, needs)) {
            try { return r.book(s, organizer, attendees, null); }
            catch (SlotTakenException raced) { /* try the next room */ }
        }
        throw new NoRoomAvailableException(s);
    }
}

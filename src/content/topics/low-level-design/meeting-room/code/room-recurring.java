record Recurrence(LocalDate firstDay, LocalDate until, Set<DayOfWeek> days, LocalTime start, Duration length, ZoneId zone) {
    /** Expand into concrete slots. Local times + zone, so 10:00 stays 10:00 across DST changes. */
    List<TimeSlot> expand() {
        List<TimeSlot> out = new ArrayList<>();
        for (LocalDate d = firstDay; !d.isAfter(until); d = d.plusDays(1)) {
            if (!days.contains(d.getDayOfWeek())) continue;
            Instant s = d.atTime(start).atZone(zone).toInstant();
            out.add(new TimeSlot(s, s.plus(length)));
        }
        return out;
    }
}

final class SeriesBooker {
    /** All-or-nothing in memory: lock the room once, check every occurrence, then insert all. */
    List<Booking> bookSeries(Room room, Recurrence rule, String organizer, Set<String> attendees) {
        String seriesId = UUID.randomUUID().toString();
        List<TimeSlot> slots = rule.expand();
        synchronized (room) {                                   // Room's methods lock on the room too (reentrant)
            List<TimeSlot> clashes = slots.stream().filter(s -> !room.isFree(s)).toList();
            if (!clashes.isEmpty()) throw new SeriesConflictException(clashes);   // let the user choose: skip or pick another room
            return slots.stream().map(s -> room.book(s, organizer, attendees, seriesId)).toList();
        }
    }
}

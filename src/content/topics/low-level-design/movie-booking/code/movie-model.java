// Static catalogue: these rarely change.
record Movie(String id, String title, Duration runtime, String language, String certificate) {}

record Theatre(String id, String name, String city) {}

record Screen(String id, String theatreId, List<Seat> layout) {}

// a physical seat
record Seat(String id, String row, int number, SeatCategory category) {}

enum SeatCategory {
    SILVER,
    GOLD,
    RECLINER
}

// A show is a movie on a screen at a time. Availability lives HERE — the same physical
// seat is free for the 6 pm show and booked for the 9 pm show.
record Show(
        String id,
        String movieId,
        String screenId,
        Instant startsAt,
        Map<SeatCategory, Money> prices) {}

enum ShowSeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}

final class ShowSeat {
    final String showId, seatId;
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;
    private String holdId; // which checkout holds it
    private Instant holdExpiresAt;

    ShowSeat(String showId, String seatId) {
        this.showId = showId;
        this.seatId = seatId;
    }

    synchronized boolean tryHold(String holdId, Instant now, Duration ttl) {
        // lazy expiry
        if (status == ShowSeatStatus.HELD && holdExpiresAt.isBefore(now))
            status = ShowSeatStatus.AVAILABLE;
        if (status != ShowSeatStatus.AVAILABLE) return false;
        status = ShowSeatStatus.HELD;
        this.holdId = holdId;
        holdExpiresAt = now.plus(ttl);
        return true;
    }

    synchronized boolean confirm(String holdId, Instant now) {
        if (status != ShowSeatStatus.HELD
                || !holdId.equals(this.holdId)
                || holdExpiresAt.isBefore(now)) return false;
        status = ShowSeatStatus.BOOKED;
        return true;
    }

    synchronized void release(String holdId) {
        if (status == ShowSeatStatus.HELD && holdId.equals(this.holdId)) {
            status = ShowSeatStatus.AVAILABLE;
            this.holdId = null;
        }
    }
}

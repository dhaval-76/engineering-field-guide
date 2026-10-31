final class BookingService {
    private static final Duration HOLD_TTL = Duration.ofMinutes(8);
    private final ShowSeatRepository seats;
    private final PaymentGateway payments;
    private final BookingRepository bookings;
    private final Clock clock;

    BookingService(
            ShowSeatRepository seats,
            PaymentGateway payments,
            BookingRepository bookings,
            Clock clock) {
        this.seats = seats;
        this.payments = payments;
        this.bookings = bookings;
        this.clock = clock;
    }

    /** Step 1 — hold all requested seats or none. Seats are taken in sorted order. */
    Hold hold(String showId, List<String> seatIds, String userId) {
        if (seatIds.isEmpty() || seatIds.size() > 10)
            throw new IllegalArgumentException("1–10 seats");
        String holdId = UUID.randomUUID().toString();
        List<ShowSeat> taken = new ArrayList<>();
        for (String seatId : seatIds.stream().sorted().toList()) {
            ShowSeat s = seats.get(showId, seatId);
            if (!s.tryHold(holdId, clock.instant(), HOLD_TTL)) {
                // roll back the partial hold
                taken.forEach(t -> t.release(holdId));
                throw new SeatUnavailableException(seatId);
            }
            taken.add(s);
        }
        return new Hold(
                holdId, showId, userId, List.copyOf(seatIds), clock.instant().plus(HOLD_TTL));
    }

    /** Step 2 — pay (idempotently, keyed by the hold), then confirm. */
    Booking checkout(Hold hold, Money amount, PaymentMethod method) {
        PaymentResult result = payments.charge(amount, method, /* idempotencyKey */ hold.id());
        if (!result.succeeded()) {
            release(hold);
            throw new PaymentFailedException(result.reason());
        }
        boolean allConfirmed =
                hold.seatIds().stream()
                        .allMatch(
                                id ->
                                        seats.get(hold.showId(), id)
                                                .confirm(hold.id(), clock.instant()));
        // hold expired while the user was paying
        if (!allConfirmed) {
            payments.refund(result.paymentId(), "hold expired");
            release(hold);
            throw new HoldExpiredException(hold.id());
        }
        return bookings.save(Booking.confirmed(hold, result.paymentId()));
    }

    void release(Hold hold) {
        hold.seatIds().forEach(id -> seats.get(hold.showId(), id).release(hold.id()));
    }
}

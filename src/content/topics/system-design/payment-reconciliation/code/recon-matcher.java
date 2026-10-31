import java.util.*;

// Amounts in minor units (paise, cents); dates as day numbers.
record Payment(String id, String ref, long amount, String currency, int day) {}

record SettlementLine(String lineId, String ref, long amount, String currency, int day) {}

enum Status { MATCHED, MATCHED_BY_AMOUNT, AMOUNT_MISMATCH, DUPLICATE, IN_TRANSIT,
              MISSING_AT_PROVIDER, MISSING_IN_LEDGER }

record Outcome(Status status, String paymentId, String lineId) {}

final class Matcher {
    static final int DATE_WINDOW = 2;   // days a settlement may lag or lead
    static final int TRANSIT_DAYS = 3;  // how long an unmatched payment may wait

    static List<Outcome> reconcile(List<Payment> ours, List<SettlementLine> theirs, int today) {
        List<Outcome> out = new ArrayList<>();
        Map<String, Payment> byRef = new HashMap<>();
        for (Payment p : ours) byRef.put(p.ref(), p);
        Set<String> matchedPayments = new HashSet<>();
        List<SettlementLine> leftover = new ArrayList<>();

        // Pass 1: the provider echoed our reference, so match on it exactly.
        for (SettlementLine l : theirs) {
            Payment p = byRef.get(l.ref());
            if (p == null) { leftover.add(l); continue; }
            if (!matchedPayments.add(p.id())) {
                out.add(new Outcome(Status.DUPLICATE, p.id(), l.lineId()));
            } else if (p.amount() != l.amount() || !p.currency().equals(l.currency())) {
                out.add(new Outcome(Status.AMOUNT_MISMATCH, p.id(), l.lineId()));
            } else {
                out.add(new Outcome(Status.MATCHED, p.id(), l.lineId()));
            }
        }

        // Pass 2: no usable reference; pair by amount and currency, closest date first.
        Map<String, List<Payment>> open = new HashMap<>();
        for (Payment p : ours)
            if (!matchedPayments.contains(p.id()))
                open.computeIfAbsent(p.amount() + p.currency(), k -> new ArrayList<>()).add(p);
        for (SettlementLine l : leftover) {
            List<Payment> candidates = open.getOrDefault(l.amount() + l.currency(), List.of());
            Payment best = null;
            for (Payment p : candidates)
                if (Math.abs(p.day() - l.day()) <= DATE_WINDOW
                        && (best == null || Math.abs(p.day() - l.day()) < Math.abs(best.day() - l.day())))
                    best = p;
            if (best == null) {
                out.add(new Outcome(Status.MISSING_IN_LEDGER, null, l.lineId()));
            } else {
                candidates.remove(best);
                matchedPayments.add(best.id());
                out.add(new Outcome(Status.MATCHED_BY_AMOUNT, best.id(), l.lineId()));
            }
        }

        // Whatever is left on our side is either still in transit or a real break.
        for (Payment p : ours)
            if (!matchedPayments.contains(p.id()))
                out.add(new Outcome(today - p.day() <= TRANSIT_DAYS
                        ? Status.IN_TRANSIT : Status.MISSING_AT_PROVIDER, p.id(), null));
        return out;
    }
}

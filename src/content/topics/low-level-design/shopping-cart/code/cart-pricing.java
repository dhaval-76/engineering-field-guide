/** Money in paise. A priced line carries the catalogue price at pricing time. */
record PricedLine(String sku, String category, int qty, long unitPaise) { long totalPaise() { return unitPaise * qty; } }

record Adjustment(String label, long amountPaise) {}             // negative = discount

record PriceBreakdown(List<PricedLine> lines, List<Adjustment> adjustments, long taxPaise, long payablePaise) {}

/** Each promotion inspects the priced cart and proposes an adjustment (or none). */
interface Promotion {
    // promotions in the same group don't stack
    String group();
    Optional<Adjustment> evaluate(List<PricedLine> lines, long runningTotalPaise);
}

record PercentOffAbove(long thresholdPaise, int percent) implements Promotion {
    public String group() { return "ORDER_PERCENT"; }
    public Optional<Adjustment> evaluate(List<PricedLine> lines, long total) {
        return total >= thresholdPaise ? Optional.of(new Adjustment(percent + "% off", -total * percent / 100)) : Optional.empty();
    }
}

record FlatCoupon(String code, long offPaise, long minOrderPaise) implements Promotion {
    public String group() { return "COUPON"; }
    public Optional<Adjustment> evaluate(List<PricedLine> lines, long total) {
        return total >= minOrderPaise ? Optional.of(new Adjustment("Coupon " + code, -Math.min(offPaise, total))) : Optional.empty();
    }
}

record BuyXGetYFree(String category, int x, int y) implements Promotion {
    public String group() { return "ITEM"; }
    public Optional<Adjustment> evaluate(List<PricedLine> lines, long total) {
        // Expand units of the category, cheapest units are the free ones.
        long[] units = lines.stream().filter(l -> l.category().equals(category))
                .flatMapToLong(l -> LongStream.generate(l::unitPaise).limit(l.qty())).sorted().toArray();
        int free = (units.length / (x + y)) * y;
        long off = Arrays.stream(units).limit(free).sum();
        return off > 0 ? Optional.of(new Adjustment("Buy " + x + " get " + y + " " + category, -off)) : Optional.empty();
    }
}

final class PricingEngine {
    private final Catalogue catalogue;
    private final TaxPolicy tax;
    PricingEngine(Catalogue catalogue, TaxPolicy tax) { this.catalogue = catalogue; this.tax = tax; }

    /** Pipeline: item-level promos → order-level promos → coupons → tax. Best offer per non-stacking group. */
    PriceBreakdown price(Cart cart, List<Promotion> active) {
        List<PricedLine> lines = cart.lines().stream().map(l -> {
            Product p = catalogue.get(l.sku());
            return new PricedLine(l.sku(), p.category(), l.qty(), p.pricePaise());
        }).toList();
        long total = lines.stream().mapToLong(PricedLine::totalPaise).sum();
        List<Adjustment> applied = new ArrayList<>();
        for (String group : List.of("ITEM", "ORDER_PERCENT", "COUPON")) {
            final long running = total;
            Optional<Adjustment> best = active.stream().filter(p -> p.group().equals(group))
                    .flatMap(p -> p.evaluate(lines, running).stream())
                    // most negative = biggest saving
                    .min(Comparator.comparingLong(Adjustment::amountPaise));
            if (best.isPresent()) { applied.add(best.get()); total += best.get().amountPaise(); }
        }
        long taxPaise = tax.on(lines, total);
        return new PriceBreakdown(lines, applied, taxPaise, total + taxPaise);
    }
}

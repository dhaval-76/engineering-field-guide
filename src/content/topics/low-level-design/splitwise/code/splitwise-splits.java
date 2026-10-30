/** Amounts are in the smallest currency unit (paise) as long — never double. */
interface SplitStrategy {
    /** Returns how much each participant owes; the values must add up to exactly `total`. */
    Map<String, Long> shares(long total, List<String> participants);
}

final class EqualSplit implements SplitStrategy {
    public Map<String, Long> shares(long total, List<String> people) {
        long base = total / people.size(), remainder = total % people.size();
        Map<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < people.size(); i++) {
            out.put(people.get(i), base + (i < remainder ? 1 : 0));    // spread leftover paise deterministically
        }
        return out;
    }
}

final class ExactSplit implements SplitStrategy {
    private final Map<String, Long> amounts;
    ExactSplit(Map<String, Long> amounts) { this.amounts = Map.copyOf(amounts); }
    public Map<String, Long> shares(long total, List<String> people) {
        long sum = amounts.values().stream().mapToLong(Long::longValue).sum();
        if (sum != total) throw new IllegalArgumentException("exact amounts add up to " + sum + ", not " + total);
        return amounts;
    }
}

final class PercentSplit implements SplitStrategy {
    private final Map<String, BigDecimal> percent;              // e.g. 33.34, 33.33, 33.33
    PercentSplit(Map<String, BigDecimal> percent) {
        if (percent.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.valueOf(100)) != 0)
            throw new IllegalArgumentException("percentages must add up to 100");
        this.percent = Map.copyOf(percent);
    }
    public Map<String, Long> shares(long total, List<String> people) {
        Map<String, Long> out = new LinkedHashMap<>();
        long assigned = 0;
        for (int i = 0; i < people.size(); i++) {
            String p = people.get(i);
            long share = i == people.size() - 1
                    ? total - assigned                              // last person absorbs rounding
                    : percent.get(p).multiply(BigDecimal.valueOf(total)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP).longValue();
            out.put(p, share);
            assigned += share;
        }
        return out;
    }
}
// ShareSplit (2 shares : 1 share …) is one more class — Expense and Group don't change.

record Transfer(String from, String to, long amount) {}

final class DebtSimplifier {
    /**
     * Greedy: repeatedly settle the largest debtor against the largest creditor.
     * Each step zeroes at least one person, so n people need at most n − 1 transfers.
     */
    static List<Transfer> simplify(Map<String, Long> netBalances) {
        Comparator<Map.Entry<String, Long>> byAmountDesc = Map.Entry.<String, Long>comparingByValue().reversed();
        PriorityQueue<Map.Entry<String, Long>> creditors = new PriorityQueue<>(byAmountDesc);
        // stored as positive debt
        PriorityQueue<Map.Entry<String, Long>> debtors = new PriorityQueue<>(byAmountDesc);
        netBalances.forEach((person, bal) -> {
            if (bal > 0) creditors.add(Map.entry(person, bal));
            else if (bal < 0) debtors.add(Map.entry(person, -bal));
        });

        List<Transfer> plan = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var c = creditors.poll();
            var d = debtors.poll();
            long amount = Math.min(c.getValue(), d.getValue());
            plan.add(new Transfer(d.getKey(), c.getKey(), amount));
            if (c.getValue() > amount) creditors.add(Map.entry(c.getKey(), c.getValue() - amount));
            if (d.getValue() > amount) debtors.add(Map.entry(d.getKey(), d.getValue() - amount));
        }
        // integer paise: no epsilon comparisons needed
        return plan;
    }
}

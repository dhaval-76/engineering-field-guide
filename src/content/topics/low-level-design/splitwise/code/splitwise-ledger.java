record Expense(String id, String paidBy, long amount, Map<String, Long> owed, String description) {}

final class Group {
    private final String id;
    private final List<Expense> expenses = new ArrayList<>();
    // + is owed money, − owes money
    private final Map<String, Long> net = new HashMap<>();

    Group(String id) { this.id = id; }

    synchronized Expense addExpense(String paidBy, long amount, List<String> participants,
                                    SplitStrategy split, String description) {
        Map<String, Long> owed = split.shares(amount, participants);
        Expense e = new Expense(UUID.randomUUID().toString(), paidBy, amount, owed, description);
        expenses.add(e);
        // payer fronted the whole amount
        net.merge(paidBy, amount, Long::sum);
        owed.forEach((person, share) -> net.merge(person, -share, Long::sum));
        return e;
    }

    // a settle-up is just a transfer
    synchronized void recordPayment(String from, String to, long amount) {
        net.merge(from, amount, Long::sum);
        net.merge(to, -amount, Long::sum);
    }

    synchronized Map<String, Long> netBalances() { return Map.copyOf(net); }
}

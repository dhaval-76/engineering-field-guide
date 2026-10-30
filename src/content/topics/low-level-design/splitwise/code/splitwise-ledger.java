record Expense(String id, String paidBy, long amount, Map<String, Long> owed, String description) {}

final class Group {
    private final String id;
    private final List<Expense> expenses = new ArrayList<>();
    private final Map<String, Long> net = new HashMap<>();      // + is owed money, − owes money

    Group(String id) { this.id = id; }

    synchronized Expense addExpense(String paidBy, long amount, List<String> participants,
                                    SplitStrategy split, String description) {
        Map<String, Long> owed = split.shares(amount, participants);
        Expense e = new Expense(UUID.randomUUID().toString(), paidBy, amount, owed, description);
        expenses.add(e);
        net.merge(paidBy, amount, Long::sum);                   // payer fronted the whole amount
        owed.forEach((person, share) -> net.merge(person, -share, Long::sum));
        return e;
    }

    synchronized void recordPayment(String from, String to, long amount) {   // a settle-up is just a transfer
        net.merge(from, amount, Long::sum);
        net.merge(to, -amount, Long::sum);
    }

    synchronized Map<String, Long> netBalances() { return Map.copyOf(net); }
}

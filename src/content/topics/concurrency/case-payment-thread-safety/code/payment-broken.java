// ✗ BROKEN — check-then-act on shared state, and money as a double
class Account {
    private double balance;

    public void debit(double amount) {
        // T1 and T2 both read 1000 and both pass the check
        if (balance >= amount) {
            balance -= amount;          // both subtract 900 → -800
        }
    }
}
// Adding `synchronized` fixes it for ONE JVM holding ONE copy of the object.
// A real service loads the account from the database per request, on several instances:
// each request has its own Account object, so a Java lock protects nothing.

// Sealed interface: the compiler knows every implementation.
sealed interface PaymentResult permits Approved, Declined, Pending {}
record Approved(String txnId, long amountPaise) implements PaymentResult {}
record Declined(String reason, boolean retryable) implements PaymentResult {}
record Pending(String txnId, Instant checkAfter) implements PaymentResult {}

// Pattern matching for switch (Java 21) + record patterns: deconstruct while matching.
String describe(PaymentResult r) {
    return switch (r) {
        case Approved(var id, var amount)            -> "paid %d paise (%s)".formatted(amount, id);
        case Declined(var reason, var retry) when retry -> "declined, retry: " + reason;
        case Declined(var reason, var ignored)       -> "declined: " + reason;
        case Pending p                               -> "pending until " + p.checkAfter();
    };   // no default: adding a new PaymentResult type makes this a compile error until handled
}

// Pattern matching for instanceof (Java 16)
if (obj instanceof String s && !s.isBlank()) System.out.println(s.length());

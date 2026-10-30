public final class Money {
    private final long amountMinor;
    private final Currency currency;

    public Money(long amountMinor, Currency currency) {
        this.amountMinor = amountMinor;
        this.currency = Objects.requireNonNull(currency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                       // fast path, and reflexive
        if (!(o instanceof Money other)) return false;    // also handles null
        return amountMinor == other.amountMinor && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountMinor, currency);       // exactly the fields used in equals()
    }
}

// Or let the compiler write all of it — records are value classes:
public record Money(long amountMinor, Currency currency) {}

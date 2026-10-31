// A record = final class, private final fields, canonical constructor, accessors,
// equals/hashCode/toString.
public record Money(long minorUnits, Currency currency) {
    // compact constructor: validate / normalise
    public Money {
        Objects.requireNonNull(currency);
        if (minorUnits < 0) throw new IllegalArgumentException("negative amount");
    }
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
        return new Money(minorUnits + other.minorUnits, currency);
    }
}

// Records are only shallowly immutable — copy mutable components:
public record Basket(String id, List<String> skus) {
    public Basket { skus = List.copyOf(skus); }
}

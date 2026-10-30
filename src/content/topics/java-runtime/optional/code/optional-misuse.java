// ✗ get() without a check — swaps NPE for NoSuchElementException
String n = findUser(id).get().name();

// ✗ isPresent()/get() — a null check in disguise
if (opt.isPresent()) { use(opt.get()); }         // ✓ opt.ifPresent(this::use)

// ✗ Optional fields and parameters
class User { Optional<String> nickname; }        // not Serializable; awkward in JPA/Jackson
void send(Optional<String> cc) { }               // callers can still pass null!
// ✓ Fields: nullable + accessor that returns Optional. Parameters: overloads or @Nullable.
class User {
    private String nickname;                                 // may be null
    Optional<String> nickname() { return Optional.ofNullable(nickname); }
}

// ✗ Optional of a collection — return an empty collection instead
Optional<List<Order>> orders();                  // ✓ List<Order> orders()  (possibly empty)

// ✗ Optional.of(maybeNull) — throws immediately if null; use ofNullable for uncertain values

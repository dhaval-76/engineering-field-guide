// local type inference (10) — still statically typed
var orders = new ArrayList<Order>();

String json = """
        {
          "id": 42,
          "status": "PAID"
        }
        """;                                       // text block (15)

// switch expression (14): no fall-through, yields a value
int days = switch (month) {
    case FEBRUARY -> leap ? 29 : 28;
    case APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30;
    default -> 31;
};

// Stream.toList (16)
List<Order> first = orders.stream().filter(Order::isPaid).toList();

// Sequenced collections (21): one API for "first/last" across List, Deque,
// LinkedHashSet/Map
SequencedCollection<String> recent = new LinkedHashSet<>(List.of("a", "b", "c"));
recent.getFirst(); recent.getLast(); recent.reversed();

// Unnamed variables (22): say "I don't need this"
try { parse(input); } catch (NumberFormatException _) { return DEFAULT; }
map.forEach((_, value) -> total += value);

// Helpful NullPointerException messages (14+): "Cannot invoke "Address.city()" because
// the return value of "Profile.address()" is null" — tells you exactly which reference
// was null.

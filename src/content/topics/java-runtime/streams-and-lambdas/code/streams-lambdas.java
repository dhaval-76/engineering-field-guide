// A lambda is an implementation of a functional interface (exactly one abstract method).
Comparator<User> byAge = (a, b) -> Integer.compare(a.age(), b.age());
Runnable task          = () -> System.out.println("run");

// The four you'll use constantly (java.util.function):
Function<String, Integer> length    = String::length;          // T → R
Predicate<Order>          isPaid    = o -> o.status() == PAID; // T → boolean
Consumer<String>          print     = System.out::println;     // T → void
Supplier<List<String>>    newList   = ArrayList::new;          // () → T

// Composition
Function<String, String> normalise = ((Function<String, String>) String::strip).andThen(s -> s.toLowerCase(Locale.ROOT));
Predicate<Order> paidAndLarge = isPaid.and(o -> o.totalPaise() > 100_000);

// Method reference forms
//   static:                       Integer::parseInt
//   instance of a given object:   System.out::println
//   instance of the parameter:    String::length          (x -> x.length())
//   constructor:                  ArrayList::new

// Captured locals must be effectively final — the lambda gets a copy of the value.
int threshold = 10;
list.removeIf(x -> x < threshold);
// threshold++;   ← would make the line above fail to compile

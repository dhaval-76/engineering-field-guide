// What `new Order(...)` does:
//   1. Load and initialise the class if needed.
//   2. Allocate memory — usually a pointer bump inside this thread's TLAB (no locking).
//   3. Zero the memory: fields start as 0 / false / null.
//   4. Run constructors (super() first), assigning real values.
//   5. Return the reference.
//
// Without synchronisation, the CPU/JIT may let another thread see the reference before step 4 finishes
// ("unsafe publication") — and read default values from a half-built object.

class Holder { int value; Holder(int v) { value = v; } }        // non-final field
static Holder shared;                                          // plain field
// Thread A: shared = new Holder(42);
// Thread B: if (shared != null) System.out.println(shared.value);   // may print 0 (!)

// final fields are the exception: the Java Memory Model guarantees any thread that sees the reference
// sees the final fields' constructed values (as long as `this` didn't escape during construction).
final class SafeHolder { final int value; SafeHolder(int v) { value = v; } }

// Other safe-publication routes: a volatile field, an AtomicReference, a static initialiser,
// putting it into a concurrent collection, or publishing under the same lock the reader uses.

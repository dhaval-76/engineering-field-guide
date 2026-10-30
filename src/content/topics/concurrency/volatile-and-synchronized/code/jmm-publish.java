// Hot-reloadable config: many readers, one writer. A volatile reference to an IMMUTABLE object.
final class ConfigHolder {
    private volatile AppConfig current = AppConfig.load();

    AppConfig get() { return current; }               // lock-free read

    void reload() {
        AppConfig next = AppConfig.load();             // build completely first…
        current = next;                                // …then publish with one volatile write
    }
}
// Readers see either the old or the new config — never a mix — because AppConfig is immutable
// and the volatile write happens-before every later volatile read of `current`.

// volatile does NOT make compound actions atomic:
private volatile int count;
void increment() { count++; }                         // ✗ still a lost-update race → use AtomicInteger

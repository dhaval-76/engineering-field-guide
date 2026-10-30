// A legacy-friendly singleton that tests can still replace.
public final class FeatureFlags {
    private static final class Holder { static final FeatureFlags INSTANCE = new FeatureFlags(loadFromFile()); }
    private static volatile FeatureFlags override;          // test seam

    private final Map<String, Boolean> flags;
    private FeatureFlags(Map<String, Boolean> flags) { this.flags = Map.copyOf(flags); }

    public static FeatureFlags get() {
        FeatureFlags o = override;
        return o != null ? o : Holder.INSTANCE;             // file is read once, lazily
    }

    public boolean isOn(String name) { return flags.getOrDefault(name, false); }

    static void overrideForTests(Map<String, Boolean> flags) { override = new FeatureFlags(flags); }
    static void resetForTests() { override = null; }

    private static Map<String, Boolean> loadFromFile() { /* read flags.json */ return Map.of(); }
}

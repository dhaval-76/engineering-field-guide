public class Registry {
    private Registry() {}

    // The JVM loads Holder (and runs its static initialiser) only on first use,
    // and class initialisation is guaranteed to happen exactly once, thread-safely.
    private static final class Holder {
        static final Registry INSTANCE = new Registry();
    }

    public static Registry getInstance() { return Holder.INSTANCE; }
}

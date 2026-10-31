public class Registry {
    // volatile: no half-built object can be seen
    private static volatile Registry instance;

    private Registry() {}

    public static Registry getInstance() {
        Registry local = instance;               // one volatile read on the fast path
        if (local == null) {
            synchronized (Registry.class) {
                local = instance;
                if (local == null) {             // re-check inside the lock
                    instance = local = new Registry();
                }
            }
        }
        return local;
    }
}

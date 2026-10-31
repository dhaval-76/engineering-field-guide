public class Registry {
    private static Registry instance;           // shared, unsynchronised
    private Registry() {}

    public static Registry getInstance() {
        if (instance == null) {                  // ① Thread A sees null
            // ② Thread B also saw null → two instances
            instance = new Registry();
        }
        return instance;
    }
}

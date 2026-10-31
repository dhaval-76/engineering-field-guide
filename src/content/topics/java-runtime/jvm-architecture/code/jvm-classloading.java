// Loading → Linking (verify, prepare, resolve) → Initialization (static initialisers),
// once per loader.
class Config {
    // runs during class initialization
    static final Map<String, String> VALUES = load();
    // first use of Config triggers this, once
    static { System.out.println("Config initialised"); }
    private static Map<String, String> load() { return Map.of("env", "prod"); }
}

// Which JAR did a class come from? (classpath conflicts)
System.out.println(com.fasterxml.jackson.databind.ObjectMapper.class
        .getProtectionDomain().getCodeSource().getLocation());
// Or start the JVM with -verbose:class (or -Xlog:class+load) to log every class load and
// its source.

// ClassNotFoundException  — you asked for a class by name (Class.forName/loadClass) and
// it isn't there. NoClassDefFoundError    — it existed at compile time but is missing (or
// failed to initialise) at runtime.

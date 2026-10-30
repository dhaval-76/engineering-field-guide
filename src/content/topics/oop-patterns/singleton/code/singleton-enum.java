// Simplest correct form in Java: safe against reflection and serialization by design.
public enum AppClock {
    INSTANCE;

    private final java.time.Clock clock = java.time.Clock.systemUTC();

    public java.time.Instant now() { return clock.instant(); }
}

// AppClock.INSTANCE.now();

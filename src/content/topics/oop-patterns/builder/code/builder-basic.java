public final class User {
    private final String name, email;          // required
    private final int age;                     // optional
    private final String phone;                // optional
    private final Role role;                   // optional, defaulted
    private final boolean verified;            // optional, defaulted

    private User(Builder b) {                  // only the builder can construct
        name = b.name; email = b.email; age = b.age;
        phone = b.phone; role = b.role; verified = b.verified;
    }

    public static Builder builder(String name, String email) { return new Builder(name, email); }

    public static final class Builder {
        private final String name, email;      // required → builder constructor
        private int age;
        private String phone;
        private Role role = Role.USER;
        private boolean verified;

        private Builder(String name, String email) { this.name = name; this.email = email; }

        public Builder age(int age)        { this.age = age; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder role(Role role)     { this.role = role; return this; }
        public Builder verified()          { this.verified = true; return this; }

        public User build() {
            if (name == null || name.isBlank()) throw new IllegalStateException("name is required");
            if (email == null || !email.contains("@")) throw new IllegalStateException("valid email is required");
            if (age < 0 || age > 150) throw new IllegalStateException("age out of range: " + age);
            // validated, immutable, safe to share between threads
            return new User(this);
        }
    }
}

User u = User.builder("Asha", "asha@example.com").age(28).role(Role.ADMIN).verified().build();

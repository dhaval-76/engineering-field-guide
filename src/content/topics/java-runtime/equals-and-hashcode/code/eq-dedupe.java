// Deduplicate users by email, case-insensitively.
public record User(String email, String name) {
    public User {
        // normalise once, so equals/hashCode stay simple
        email = email.trim().toLowerCase(Locale.ROOT);
    }
    // A record's equals/hashCode use ALL components — here that includes name.
    // If identity is "email only", override both to use only email:
    @Override public boolean equals(Object o) {
        return o instanceof User u && u.email.equals(email);
    }
    @Override public int hashCode() { return email.hashCode(); }
}

Set<User> unique = new HashSet<>(List.of(new User("A@x.com", "A"), new User("a@x.com", "Asha")));
unique.size();   // 1

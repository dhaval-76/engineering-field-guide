// Which argument is the phone number? What do the nulls and booleans mean?
new User("Asha", null, null, "asha@example.com", 28, true, null, "ADMIN");

// Telescoping constructors: one overload per combination of optional fields.
public User(String name, String email) { this(name, email, 0); }
public User(String name, String email, int age) { this(name, email, age, null); }
public User(String name, String email, int age, String phone) { /* ... */ }
// ...and so on. Setters instead would leave the object half-built and mutable.

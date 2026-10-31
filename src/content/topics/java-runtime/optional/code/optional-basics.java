// ✗ A null return is an invisible contract
public User findUser(String id) { return users.get(id); }       // may return null
// NPE — far from the real cause
findUser("42").name();

// ✓ The return type says "might be absent"
public Optional<User> findUser(String id) { return Optional.ofNullable(users.get(id)); }

Optional<User> u = findUser("42");
// transform, then default
String name   = u.map(User::name).orElse("Guest");
// absence is an error here
User   user   = u.orElseThrow(() -> new UserNotFound("42"));
u.ifPresentOrElse(this::greet, this::showSignUp);                // act on either case

// orElse vs orElseGet: orElse's argument is ALWAYS evaluated
// ✗ queries the DB even when the user exists
User a = u.orElse(loadGuestFromDb());
User b = u.orElseGet(this::loadGuestFromDb);    // ✓ only runs when empty

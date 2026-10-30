// ✗ A null return is an invisible contract
public User findUser(String id) { return users.get(id); }       // may return null
findUser("42").name();                                            // NPE — far from the real cause

// ✓ The return type says "might be absent"
public Optional<User> findUser(String id) { return Optional.ofNullable(users.get(id)); }

Optional<User> u = findUser("42");
String name   = u.map(User::name).orElse("Guest");               // transform, then default
User   user   = u.orElseThrow(() -> new UserNotFound("42"));    // absence is an error here
u.ifPresentOrElse(this::greet, this::showSignUp);                // act on either case

// orElse vs orElseGet: orElse's argument is ALWAYS evaluated
User a = u.orElse(loadGuestFromDb());          // ✗ queries the DB even when the user exists
User b = u.orElseGet(this::loadGuestFromDb);    // ✓ only runs when empty

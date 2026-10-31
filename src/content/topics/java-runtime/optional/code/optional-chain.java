// When each step may be absent, chain instead of nesting null checks.
public String cityOf(String userId) {
    return userRepo.findById(userId)            // Optional<User>
            // profile() returns Optional<Profile> → flatMap
            .flatMap(User::profile)
            .flatMap(Profile::address)          // Optional<Address>
            .map(Address::city)                 // city() returns String → map
            .filter(c -> !c.isBlank())
            .orElse("Unknown");
}

// map vs flatMap:
//   opt.map(f)     — f returns a plain value  → Optional<R>
// opt.flatMap(f) — f returns Optional<R>    → Optional<R>   (map would give
// Optional<Optional<R>>)

// Streams of optionals: keep the present values
List<Address> addresses = users.stream().map(User::profile).flatMap(Optional::stream)
        .map(Profile::address).flatMap(Optional::stream).toList();

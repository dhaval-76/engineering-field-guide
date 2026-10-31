// parallelStream splits work across the common ForkJoinPool (≈ number of CPU cores).

// ✓ Good candidate: large, CPU-bound, independent work on a splittable source (ArrayList,
// arrays, ranges)
long primes = LongStream.rangeClosed(2, 50_000_000).parallel().filter(Primes::isPrime).count();

// ✗ Blocking I/O: occupies the shared common pool — slowing every other parallel stream
// in the JVM
users.parallelStream().map(u -> httpClient.fetchProfile(u.id())).toList();
// ✓ Use an explicit executor (or virtual threads) for I/O-bound fan-out instead.

// ✗ Shared mutable state: ArrayList isn't thread-safe → lost or corrupted elements
List<Integer> out = new ArrayList<>();
numbers.parallelStream().map(n -> n * 2).forEach(out::add);
// ✓ Let the collector combine results
List<Integer> safe = numbers.parallelStream().map(n -> n * 2).toList();

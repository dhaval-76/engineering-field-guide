// A one-method strategy interface is a functional interface — lambdas are strategies.
@FunctionalInterface
interface RetryBackoff { Duration delayFor(int attempt); }

RetryBackoff fixed       = attempt -> Duration.ofMillis(200);
RetryBackoff exponential = attempt -> Duration.ofMillis(100L << Math.min(attempt, 10));
RetryBackoff jittered    = attempt -> Duration.ofMillis(ThreadLocalRandom.current().nextLong(100L << Math.min(attempt, 10)));

// The JDK is full of strategies: Comparator, Executor, RejectedExecutionHandler,
// java.time.Clock…
list.sort(Comparator.comparing(Order::createdAt).reversed());

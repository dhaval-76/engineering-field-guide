enum Status {
    CREATED,
    ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    ATTEMPT_FAILED,
    RETURNED
}

record Agent(String id, double rating) {}

class Shipment {
    final String id;
    final boolean prime;
    final long createdAt;
    Status status = Status.CREATED;
    Agent agent;
    int failedAttempts;
    final List<String> history = new ArrayList<>(); // tracking events

    Shipment(String id, boolean prime, long createdAt) {
        this.id = id;
        this.prime = prime;
        this.createdAt = createdAt;
        history.add("CREATED");
    }
}

/** Strategy: who gets a package. The Prime follow-up is a new strategy, not new branches. */
interface AssignmentStrategy {
    Agent pick(Shipment s, List<Agent> free);
}

class PrimeGetsBestRated implements AssignmentStrategy {
    public Agent pick(Shipment s, List<Agent> free) {
        Comparator<Agent> byRating = Comparator.comparingDouble(Agent::rating);
        return s.prime ? Collections.max(free, byRating) : Collections.min(free, byRating);
    }
}

class DeliveryService {
    private static final Map<Status, Set<Status>> ALLOWED =
            Map.of(
                    Status.CREATED,
                    Set.of(Status.ASSIGNED),
                    Status.ASSIGNED,
                    Set.of(Status.OUT_FOR_DELIVERY),
                    Status.OUT_FOR_DELIVERY,
                    Set.of(Status.DELIVERED, Status.ATTEMPT_FAILED),
                    Status.ATTEMPT_FAILED,
                    Set.of(Status.ASSIGNED, Status.RETURNED),
                    Status.DELIVERED,
                    Set.of(),
                    Status.RETURNED,
                    Set.of());
    static final int MAX_ATTEMPTS = 3;

    // Prime first, then oldest first.
    private final PriorityQueue<Shipment> waiting =
            new PriorityQueue<>(
                    Comparator.comparing((Shipment s) -> !s.prime)
                            .thenComparingLong(s -> s.createdAt));
    private final List<Agent> freeAgents = new ArrayList<>();
    private final AssignmentStrategy strategy;

    DeliveryService(AssignmentStrategy strategy) {
        this.strategy = strategy;
    }

    void agentAvailable(Agent a) {
        freeAgents.add(a);
    }

    void enqueue(Shipment s) {
        waiting.add(s);
    }

    void assignAll() {
        while (!waiting.isEmpty() && !freeAgents.isEmpty()) {
            Shipment s = waiting.poll();
            Agent a = strategy.pick(s, freeAgents);
            freeAgents.remove(a);
            s.agent = a;
            move(s, Status.ASSIGNED);
        }
    }

    void move(Shipment s, Status next) { // the state machine guards every update
        if (!ALLOWED.get(s.status).contains(next)) {
            throw new IllegalStateException(s.status + " -> " + next);
        }
        s.status = next;
        s.history.add(next.name());
        if (next == Status.ATTEMPT_FAILED) {
            freeAgents.add(s.agent);
            if (++s.failedAttempts >= MAX_ATTEMPTS) move(s, Status.RETURNED);
            else waiting.add(s); // try again on the next run
        }
        if (next == Status.DELIVERED) freeAgents.add(s.agent);
    }
}

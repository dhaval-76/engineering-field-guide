/** A small HNSW index over vectors, using squared Euclidean distance. Not thread-safe. */
class Hnsw {
    private final int m; // links per node on upper layers; layer 0 allows 2 * m
    private final int efConstruction;
    private final double levelFactor;
    private final Random random;
    private final List<float[]> vectors = new ArrayList<>();
    // node -> layer -> neighbours
    private final List<List<List<Integer>>> links = new ArrayList<>();
    private int entry = -1;
    long distanceCalls;

    record Hit(int id, double dist) {}

    Hnsw(int m, int efConstruction, long seed) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.levelFactor = 1 / Math.log(m);
        this.random = new Random(seed);
    }

    double distance(float[] a, float[] b) {
        distanceCalls++;
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += (a[i] - b[i]) * (a[i] - b[i]);
        return sum;
    }

    /** Most nodes stay on layer 0; each higher layer holds about 1/m of the one below. */
    private int randomLevel() {
        return (int) (-Math.log(1 - random.nextDouble()) * levelFactor);
    }

    private int topLevel(int node) {
        return links.get(node).size() - 1;
    }

    /** Best-first search of one layer, returning up to ef hits, nearest first. */
    private List<Hit> searchLayer(float[] query, int start, int ef, int layer) {
        Comparator<Hit> byDist = Comparator.comparingDouble(Hit::dist);
        Set<Integer> visited = new HashSet<>(List.of(start));
        Hit first = new Hit(start, distance(query, vectors.get(start)));
        PriorityQueue<Hit> frontier = new PriorityQueue<>(byDist);
        PriorityQueue<Hit> best = new PriorityQueue<>(byDist.reversed());
        frontier.add(first);
        best.add(first);
        while (!frontier.isEmpty()) {
            Hit current = frontier.poll();
            // Everything left to explore is further than the worst result we are keeping.
            if (best.size() >= ef && current.dist() > best.peek().dist()) break;
            for (int next : links.get(current.id()).get(layer)) {
                if (!visited.add(next)) continue;
                double d = distance(query, vectors.get(next));
                if (best.size() < ef || d < best.peek().dist()) {
                    Hit hit = new Hit(next, d);
                    frontier.add(hit);
                    best.add(hit);
                    if (best.size() > ef) best.poll();
                }
            }
        }
        List<Hit> result = new ArrayList<>(best);
        result.sort(byDist);
        return result;
    }

    /** Walks down from the top layer to just above `target`, keeping only the closest node. */
    private int descend(float[] query, int target) {
        int node = entry;
        for (int layer = topLevel(entry); layer > target; layer--) {
            node = searchLayer(query, node, 1, layer).get(0).id();
        }
        return node;
    }

    int add(float[] vector) {
        int id = vectors.size();
        int level = randomLevel();
        vectors.add(vector);
        List<List<Integer>> own = new ArrayList<>();
        for (int i = 0; i <= level; i++) own.add(new ArrayList<>());
        links.add(own);
        if (entry == -1) {
            entry = id;
            return id;
        }
        int oldTop = topLevel(entry);
        int node = descend(vector, level);
        for (int layer = Math.min(level, oldTop); layer >= 0; layer--) {
            List<Hit> near = searchLayer(vector, node, efConstruction, layer);
            int limit = layer == 0 ? 2 * m : m;
            for (Hit hit : near.subList(0, Math.min(m, near.size()))) {
                own.get(layer).add(hit.id());
                List<Integer> theirs = links.get(hit.id()).get(layer);
                theirs.add(id);
                if (theirs.size() > limit) prune(hit.id(), theirs, limit);
            }
            node = near.get(0).id();
        }
        if (level > oldTop) entry = id;
        return id;
    }

    /** Keeps a node's closest neighbours when it has gained too many links. */
    private void prune(int node, List<Integer> neighbours, int limit) {
        float[] from = vectors.get(node);
        List<Integer> kept =
                neighbours.stream()
                        .sorted(Comparator.comparingDouble(n -> distance(from, vectors.get(n))))
                        .limit(limit)
                        .toList();
        neighbours.clear();
        neighbours.addAll(kept);
    }

    List<Hit> search(float[] query, int k, int efSearch) {
        if (entry == -1) return List.of();
        List<Hit> near = searchLayer(query, descend(query, 0), Math.max(efSearch, k), 0);
        return near.subList(0, Math.min(k, near.size()));
    }
}

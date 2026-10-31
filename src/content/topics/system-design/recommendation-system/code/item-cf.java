final class ItemCF {
    record Neighbour(String item, double sim) {}

    /** Offline batch job: cosine similarity from co-occurrence; keep the top N neighbours per item. */
    static Map<String, List<Neighbour>> itemNeighbours(Map<String, Set<String>> interactions, int topN) {
        Map<String, Integer> usersOf = new HashMap<>();
        Map<String, Map<String, Integer>> co = new HashMap<>();
        for (Set<String> items : interactions.values())
            for (String i : items) {
                usersOf.merge(i, 1, Integer::sum);
                for (String j : items) if (!i.equals(j)) co.computeIfAbsent(i, x -> new HashMap<>()).merge(j, 1, Integer::sum);
            }
        Map<String, List<Neighbour>> out = new HashMap<>();
        co.forEach((i, js) -> out.put(i, js.entrySet().stream()
                .map(e -> new Neighbour(e.getKey(), e.getValue() / Math.sqrt((double) usersOf.get(i) * usersOf.get(e.getKey()))))
                .sorted(Comparator.comparingDouble(Neighbour::sim).reversed().thenComparing(Neighbour::item))
                .limit(topN).toList()));
        return out;
    }

    /** Online: score unseen items by summed similarity to the user's history. */
    static List<String> recommend(Set<String> history, Map<String, List<Neighbour>> neighbours, int k) {
        Map<String, Double> score = new HashMap<>();
        for (String i : history)
            for (Neighbour n : neighbours.getOrDefault(i, List.of()))
                if (!history.contains(n.item())) score.merge(n.item(), n.sim(), Double::sum);
        return score.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(e -> -Math.round(e.getValue() * 1e12) / 1e12)
                        .thenComparing(Map.Entry::getKey))
                .limit(k).map(Map.Entry::getKey).toList();
    }
}

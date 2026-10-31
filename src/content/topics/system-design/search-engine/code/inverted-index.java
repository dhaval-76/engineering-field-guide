/** In-memory inverted index with BM25 ranking — the core of what Lucene does, minus compression and disk. */
final class InvertedIndex {
    private static final double K1 = 1.2, B = 0.75;                     // BM25 defaults
    private static final Set<String> STOP = Set.of("the", "a", "an", "and", "of", "on", "to", "in");

    // term → (docId → tf)
    private final Map<String, Map<String, Integer>> postings = new HashMap<>();
    private final Map<String, Integer> docLength = new HashMap<>();
    private long totalLength;

    void index(String docId, String text) {
        List<String> terms = analyse(text);
        docLength.put(docId, terms.size());
        totalLength += terms.size();
        for (String t : terms) postings.computeIfAbsent(t, k -> new HashMap<>()).merge(docId, 1, Integer::sum);
    }

    List<Map.Entry<String, Double>> search(String query, int k) {
        int n = docLength.size();
        double avgLen = (double) totalLength / Math.max(1, n);
        Map<String, Double> scores = new HashMap<>();
        for (String term : analyse(query)) {
            Map<String, Integer> docs = postings.getOrDefault(term, Map.of());
            // rare terms weigh more
            double idf = Math.log(1 + (n - docs.size() + 0.5) / (docs.size() + 0.5));
            docs.forEach((doc, tf) -> {
                // saturation + length
                double norm = tf * (K1 + 1) / (tf + K1 * (1 - B + B * docLength.get(doc) / avgLen));
                scores.merge(doc, idf * norm, Double::sum);
            });
        }
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(k).toList();
    }

    /** The SAME analysis for documents and queries. */
    static List<String> analyse(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(t -> !t.isBlank() && !STOP.contains(t))
            .map(InvertedIndex::stem)
            .toList();
    }

    // toy stemmer; real ones: Porter/Snowball
    private static String stem(String t) {
        for (String suffix : List.of("ing", "es", "s")) if (t.length() > suffix.length() + 2 && t.endsWith(suffix))
            return t.substring(0, t.length() - suffix.length());
        return t;
    }
}

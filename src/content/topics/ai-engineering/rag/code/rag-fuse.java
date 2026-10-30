record Hit(String chunkId, String text) {}

/** Reciprocal rank fusion: score = Σ 1 / (k + rank). Rewards chunks ranked high by either retriever. */
List<Hit> fuse(List<Hit> vectorHits, List<Hit> keywordHits, int topN) {
    final int k = 60;
    Map<String, Double> score = new HashMap<>();
    Map<String, Hit> byId = new HashMap<>();
    for (List<Hit> list : List.of(vectorHits, keywordHits)) {
        for (int rank = 0; rank < list.size(); rank++) {
            Hit h = list.get(rank);
            score.merge(h.chunkId(), 1.0 / (k + rank + 1), Double::sum);
            byId.putIfAbsent(h.chunkId(), h);
        }
    }
    return score.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topN)
            .map(e -> byId.get(e.getKey()))
            .toList();
}

/** Grounded prompt: chunks are labelled with ids so the model can cite them. */
String buildPrompt(String question, List<Hit> chunks) {
    StringBuilder sb = new StringBuilder("""
        Answer the question using only the sources below.
        Cite sources like [id]. If the sources don't contain the answer, say you don't know.

        """);
    for (Hit h : chunks) sb.append("<source id=\"").append(h.chunkId()).append("\">\n")
            .append(h.text()).append("\n</source>\n");
    return sb.append("\nQuestion: ").append(question).toString();
}

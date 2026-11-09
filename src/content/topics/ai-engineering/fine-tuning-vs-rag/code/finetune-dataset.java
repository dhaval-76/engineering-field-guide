record Turn(String role, String content) {}

record Example(List<Turn> turns) {
    String firstUserText() {
        return turns.stream()
                .filter(t -> t.role().equals("user"))
                .map(Turn::content)
                .findFirst()
                .orElse("");
    }
}

record Split(List<Example> train, List<Example> validation) {}

/** Problems that would waste a training run. An empty list means the example is usable. */
List<String> validate(Example e) {
    List<String> problems = new ArrayList<>();
    if (e.turns().isEmpty()) return List.of("no turns");
    if (e.firstUserText().isBlank()) problems.add("no user message");
    Turn last = e.turns().get(e.turns().size() - 1);
    if (!last.role().equals("assistant") || last.content().isBlank()) {
        problems.add("must end with a non-empty assistant message");
    }
    return problems;
}

String normalize(String text) {
    return text.trim().toLowerCase().replaceAll("\\s+", " ");
}

/**
 * Splits by a hash of the question, so the same example always lands on the same side. A random
 * split lets near-identical questions leak into validation and inflate the score.
 */
Split split(List<Example> examples, int validationPercent) throws NoSuchAlgorithmException {
    Map<String, Example> unique = new LinkedHashMap<>();
    for (Example e : examples) unique.putIfAbsent(normalize(e.firstUserText()), e);
    List<Example> train = new ArrayList<>();
    List<Example> validation = new ArrayList<>();
    for (var entry : unique.entrySet()) {
        byte[] hash =
                MessageDigest.getInstance("SHA-256")
                        .digest(entry.getKey().getBytes(StandardCharsets.UTF_8));
        int bucket = (hash[0] & 0xff) % 100;
        (bucket < validationPercent ? validation : train).add(entry.getValue());
    }
    return new Split(train, validation);
}

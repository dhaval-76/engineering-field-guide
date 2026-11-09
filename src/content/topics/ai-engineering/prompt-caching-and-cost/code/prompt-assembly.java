record Tool(String name, String schema) {}

record Message(String role, String text) {}

/**
 * Renders a prompt with the most stable parts first, so successive requests share the longest
 * possible prefix. Anything that changes per request goes last.
 */
List<Message> assemble(
        String instructions,
        List<Tool> tools,
        List<Message> history,
        String retrieved,
        String question,
        LocalDate today) {
    // Sorted: the same tools in a different order would be a different prefix.
    String toolBlock =
            tools.stream()
                    .sorted(Comparator.comparing(Tool::name))
                    .map(t -> t.name() + ": " + t.schema())
                    .collect(Collectors.joining("\n"));
    List<Message> prompt = new ArrayList<>();
    prompt.add(new Message("system", instructions + "\n\nTools:\n" + toolBlock));
    prompt.addAll(history);
    // Today's date lives here, at the end. In the system prompt it would break the cache daily.
    prompt.add(new Message("user", "Date: " + today + "\n\n" + retrieved + "\n\n" + question));
    return prompt;
}

/** For a test: how many leading messages two rendered prompts share. */
int sharedPrefix(List<Message> a, List<Message> b) {
    int n = 0;
    while (n < a.size() && n < b.size() && a.get(n).equals(b.get(n))) n++;
    return n;
}

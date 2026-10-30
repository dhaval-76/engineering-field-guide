/** Split text into ~maxWords-word chunks on paragraph boundaries, carrying `overlap` words forward. */
List<String> chunk(String text, int maxWords, int overlap) {
    List<String> chunks = new ArrayList<>();
    List<String> current = new ArrayList<>();
    for (String para : text.split("\\n\\s*\\n")) {
        List<String> words = List.of(para.trim().split("\\s+"));
        if (words.get(0).isEmpty()) continue;
        int step = maxWords - overlap;                                 // overlap must be < maxWords
        for (int i = 0; i < words.size(); i += step) {               // split very long paragraphs
            List<String> piece = words.subList(i, Math.min(words.size(), i + step));
            if (!current.isEmpty() && current.size() + piece.size() > maxWords) {
                chunks.add(String.join(" ", current));
                current = new ArrayList<>(current.subList(Math.max(0, current.size() - overlap), current.size()));
            }
            current.addAll(piece);
        }
    }
    if (!current.isEmpty()) chunks.add(String.join(" ", current));
    return chunks;
}

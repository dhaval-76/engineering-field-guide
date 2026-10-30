/**
 * Piece table: the original text is never modified; inserted text is appended to an "add" buffer;
 * the document is a list of pieces pointing into either buffer. Edits only split/insert pieces.
 */
final class PieceTable implements Buffer {
    private record Piece(boolean fromAdd, int start, int length) {}

    private final String original;
    private final StringBuilder add = new StringBuilder();
    private final List<Piece> pieces = new ArrayList<>();       // a balanced tree in real editors

    PieceTable(String original) {
        this.original = original;
        if (!original.isEmpty()) pieces.add(new Piece(false, 0, original.length()));
    }

    public void insert(int pos, String text) {
        Piece inserted = new Piece(true, add.length(), text.length());
        add.append(text);                                        // append-only: cheap, and old pieces stay valid
        int offset = 0;
        for (int i = 0; i <= pieces.size(); i++) {
            if (i == pieces.size()) { pieces.add(inserted); return; }
            Piece p = pieces.get(i);
            if (pos <= offset + p.length()) {
                int split = pos - offset;                        // split p into [left][inserted][right]
                List<Piece> replacement = new ArrayList<>();
                if (split > 0) replacement.add(new Piece(p.fromAdd(), p.start(), split));
                replacement.add(inserted);
                if (split < p.length()) replacement.add(new Piece(p.fromAdd(), p.start() + split, p.length() - split));
                pieces.remove(i);
                pieces.addAll(i, replacement);
                return;
            }
            offset += p.length();
        }
    }

    public String delete(int pos, int len) { /* split pieces at pos and pos+len, drop the middle ones */ return ""; }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) sb.append(p.fromAdd() ? add : original, p.start(), p.start() + p.length());
        return sb.toString();
    }
}

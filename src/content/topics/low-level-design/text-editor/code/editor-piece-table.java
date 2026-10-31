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

    /** Keep the parts of each piece outside [pos, pos + len); return the removed text (for undo). */
    public String delete(int pos, int len) {
        StringBuilder removed = new StringBuilder();
        List<Piece> kept = new ArrayList<>();
        int offset = 0, end = pos + len;
        for (Piece p : pieces) {
            int pStart = offset, pEnd = offset + p.length();
            offset = pEnd;
            int cutFrom = Math.max(pos, pStart), cutTo = Math.min(end, pEnd);
            if (cutFrom >= cutTo) { kept.add(p); continue; }                  // untouched
            CharSequence buf = p.fromAdd() ? add : original;
            removed.append(buf, p.start() + cutFrom - pStart, p.start() + cutTo - pStart);
            if (cutFrom > pStart) kept.add(new Piece(p.fromAdd(), p.start(), cutFrom - pStart));            // left part
            if (cutTo < pEnd) kept.add(new Piece(p.fromAdd(), p.start() + cutTo - pStart, pEnd - cutTo));   // right part
        }
        pieces.clear();
        pieces.addAll(kept);
        return removed.toString();
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) sb.append(p.fromAdd() ? add : original, p.start(), p.start() + p.length());
        return sb.toString();
    }
}

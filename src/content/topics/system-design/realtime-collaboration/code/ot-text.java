sealed interface Op permits Insert, Delete {}
record Insert(int pos, String text) implements Op {}
record Delete(int pos, int len) implements Op {}

final class Ot {
    /** Rewrite `op` so it applies AFTER `applied` (both were made against the same revision). */
    static Op transform(Op op, Op applied, boolean opWinsTies) {
        return switch (op) {
            case Insert ins -> switch (applied) {
                case Insert a -> (a.pos() < ins.pos() || (a.pos() == ins.pos() && !opWinsTies))
                        ? new Insert(ins.pos() + a.text().length(), ins.text()) : ins;
                case Delete d -> ins.pos() <= d.pos() ? ins
                        : ins.pos() >= d.pos() + d.len() ? new Insert(ins.pos() - d.len(), ins.text())
                        // typed inside a deleted range: dropped (matches the rule below)
                        : new Insert(d.pos(), "");
            };
            case Delete del -> switch (applied) {
                case Insert a -> a.pos() <= del.pos()
                        ? new Delete(del.pos() + a.text().length(), del.len())
                        : a.pos() >= del.pos() + del.len() ? del
                        // the delete swallows text typed inside it
                        : new Delete(del.pos(), del.len() + a.text().length());
                case Delete d -> transformDelete(del, d);
            };
        };
    }

    private static Op transformDelete(Delete del, Delete d) {
        int start = del.pos(), end = del.pos() + del.len();
        int dStart = d.pos(), dEnd = d.pos() + d.len();
        if (end <= dStart) return del;                                  // entirely before
        // entirely after
        if (start >= dEnd) return new Delete(start - d.len(), del.len());
        // part already deleted
        int overlap = Math.min(end, dEnd) - Math.max(start, dStart);
        return new Delete(Math.min(start, dStart), del.len() - overlap);
    }

    static String apply(String doc, Op op) {
        return switch (op) {
            case Insert i -> doc.substring(0, i.pos()) + i.text() + doc.substring(i.pos());
            case Delete d -> d.len() == 0 ? doc : doc.substring(0, d.pos()) + doc.substring(d.pos() + d.len());
        };
    }
}
// Convergence check: for concurrent a and b from the same doc,
// apply(apply(doc, a), transform(b, a, false)) equals apply(apply(doc, b), transform(a,
// b, true))

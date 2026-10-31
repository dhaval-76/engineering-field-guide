/**
 * A rank strictly between two ranks, using lowercase letters as base-26 digits. `after ==
 * null` means "end of column". Generated ranks never end in 'a', so there is always room.
 */
static String rankBetween(String before, String after) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; ; i++) {
        int lo = i < before.length() ? before.charAt(i) - 'a' : 0;
        int hi = after != null && i < after.length() ? after.charAt(i) - 'a' : 26;
        // room at this position: take the midpoint
        if (hi - lo > 1) {
            out.append((char) ('a' + (lo + hi) / 2));
            return out.toString();
        }
        // no room: keep this digit and look one position deeper
        out.append((char) ('a' + lo));
        // we're now below `after`, so the upper bound opens up
        if (hi - lo == 1) after = null;
    }
}
// rankBetween("c", "m")  → "h"
// rankBetween("h", "i")  → "hn"
// rankBetween("", "b")   → "an"     (top of the column)
// rankBetween("t", null) → "w"      (bottom of the column)

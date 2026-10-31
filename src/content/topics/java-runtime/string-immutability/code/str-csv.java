/**
 * RFC 4180: quote a field if it contains a comma, quote or newline; double any quotes
 * inside.
 */
static String toCsvRow(List<String> fields) {
    StringBuilder sb = new StringBuilder(fields.size() * 16);
    for (int i = 0; i < fields.size(); i++) {
        if (i > 0) sb.append(',');
        String f = fields.get(i);
        if (f == null) continue;                                   // null → empty field
        boolean quote = f.indexOf(',') >= 0 || f.indexOf('"') >= 0
                || f.indexOf('\n') >= 0 || f.indexOf('\r') >= 0;
        if (!quote) { sb.append(f); continue; }
        sb.append('"').append(f.replace("\"", "\"\"")).append('"');
    }
    return sb.toString();
}
// toCsvRow(List.of("a", "b,c", "say \"hi\"", null))  →  a,"b,c","say ""hi""",

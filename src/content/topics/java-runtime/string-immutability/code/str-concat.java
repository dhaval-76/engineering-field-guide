// ✗ Quadratic: each += copies everything built so far into a new String.
String csv = "";
for (Row r : rows) csv += r.toCsv() + "\n";          // 50,000 rows → ~1.25 billion characters copied

// ✓ Linear: one growable buffer, one final String.
StringBuilder sb = new StringBuilder(rows.size() * 64);   // pre-size if you can estimate
for (Row r : rows) sb.append(r.toCsv()).append('\n');
String csv = sb.toString();

// ✓ Or let a collector / joiner do it:
String csv = rows.stream().map(Row::toCsv).collect(Collectors.joining("\n"));

// A single expression like  "Hi " + name + "!"  is fine: since Java 9 the compiler emits an
// invokedynamic call (StringConcatFactory) that sizes the result once. Only loops are the problem.

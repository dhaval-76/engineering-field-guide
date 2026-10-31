/** A tiny formula language: numbers, cell refs, + - * / and SUM(A1:A9). */
sealed interface Expr {
    double eval(ToDoubleFunction<String> cell);
    Set<String> references();

    record Num(double v) implements Expr {
        public double eval(ToDoubleFunction<String> c) { return v; }
        public Set<String> references() { return Set.of(); }
    }
    record Ref(String id) implements Expr {
        public double eval(ToDoubleFunction<String> c) { return c.applyAsDouble(id); }
        public Set<String> references() { return Set.of(id); }
    }
    record Bin(char op, Expr l, Expr r) implements Expr {
        public double eval(ToDoubleFunction<String> c) {
            double a = l.eval(c), b = r.eval(c);
            return switch (op) { case '+' -> a + b; case '-' -> a - b; case '*' -> a * b; default -> a / b; };
        }
        public Set<String> references() { var s = new HashSet<>(l.references()); s.addAll(r.references()); return s; }
    }
    record Sum(List<String> range) implements Expr {
        public double eval(ToDoubleFunction<String> c) { return range.stream().mapToDouble(c).sum(); }
        public Set<String> references() { return Set.copyOf(range); }
    }
}

/**
 * Recursive-descent parser: expr := term (('+'|'-') term)*, term := factor (('*'|'/')
 * factor)*.
 */
final class Parser {
    private final String s; private int i;
    private Parser(String s) { this.s = s.replace(" ", ""); }
    static Expr parse(String s) { Parser p = new Parser(s); Expr e = p.expr(); if (p.i != p.s.length()) throw p.error(); return e; }

    private Expr expr() { Expr e = term(); while (peek('+') || peek('-')) { char op = s.charAt(i++); e = new Expr.Bin(op, e, term()); } return e; }
    private Expr term() { Expr e = factor(); while (peek('*') || peek('/')) { char op = s.charAt(i++); e = new Expr.Bin(op, e, factor()); } return e; }
    private Expr factor() {
        if (peek('(')) { i++; Expr e = expr(); expect(')'); return e; }
        if (s.startsWith("SUM(", i)) { i += 4; String from = ref(); expect(':'); String to = ref(); expect(')'); return new Expr.Sum(Ranges.expand(from, to)); }
        if (i < s.length() && Character.isLetter(s.charAt(i))) return new Expr.Ref(ref());
        int start = i; while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        if (start == i) throw error();
        return new Expr.Num(Double.parseDouble(s.substring(start, i)));
    }
    private String ref() { int start = i; while (i < s.length() && Character.isLetterOrDigit(s.charAt(i))) i++; return s.substring(start, i); }
    private boolean peek(char c) { return i < s.length() && s.charAt(i) == c; }
    private void expect(char c) { if (!peek(c)) throw error(); i++; }
    private IllegalArgumentException error() { return new IllegalArgumentException("bad formula near position " + i + ": " + s); }
}

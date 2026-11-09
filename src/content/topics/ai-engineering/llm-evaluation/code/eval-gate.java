record EvalCase(String id, String input, Predicate<String> passes) {}

record EvalRun(int total, Set<String> failed) {
    int passed() {
        return total - failed.size();
    }
}

record Comparison(Set<String> newFailures, Set<String> fixed, double pValue) {}

/** Runs every case once and records which ones fail. */
EvalRun run(List<EvalCase> cases, UnaryOperator<String> model) {
    Set<String> failed = new TreeSet<>();
    for (EvalCase c : cases) {
        if (!c.passes().test(model.apply(c.input()))) failed.add(c.id());
    }
    return new EvalRun(cases.size(), failed);
}

/** 95% Wilson interval for a pass rate: how far the true rate may be from what you measured. */
double[] wilson(int passed, int total) {
    double z = 1.96, p = (double) passed / total, n = total;
    double centre = (p + z * z / (2 * n)) / (1 + z * z / n);
    double margin = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n);
    return new double[] {centre - margin, centre + margin};
}

/** Two-sided sign test on the cases that changed; the cases that did not change carry no signal. */
double signTest(int worse, int better) {
    int n = worse + better;
    if (n == 0) return 1;
    double tail = 0, term = Math.pow(0.5, n); // term = C(n, i) * 0.5^n
    for (int i = 0; i <= Math.min(worse, better); i++) {
        tail += term;
        term = term * (n - i) / (i + 1);
    }
    return Math.min(1, 2 * tail);
}

/** Compares the same cases under two versions. Matching cases makes small changes detectable. */
Comparison compare(EvalRun baseline, EvalRun candidate) {
    Set<String> newFailures = new TreeSet<>(candidate.failed());
    newFailures.removeAll(baseline.failed());
    Set<String> fixed = new TreeSet<>(baseline.failed());
    fixed.removeAll(candidate.failed());
    return new Comparison(newFailures, fixed, signTest(newFailures.size(), fixed.size()));
}

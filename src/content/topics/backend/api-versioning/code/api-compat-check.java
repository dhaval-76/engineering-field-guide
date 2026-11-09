record FieldSpec(String type, boolean required) {}

enum Direction {
    REQUEST,
    RESPONSE
}

/** Lists what would break existing clients. Run it in CI against the published contract. */
List<String> breakingChanges(
        Map<String, FieldSpec> before, Map<String, FieldSpec> after, Direction direction) {
    List<String> problems = new ArrayList<>();
    before.forEach(
            (name, old) -> {
                FieldSpec now = after.get(name);
                if (now == null) {
                    problems.add("removed: " + name);
                } else if (!now.type().equals(old.type())) {
                    problems.add("type changed: " + name);
                } else if (direction == Direction.REQUEST && now.required() && !old.required()) {
                    problems.add("now required: " + name);
                } else if (direction == Direction.RESPONSE && old.required() && !now.required()) {
                    problems.add("no longer guaranteed: " + name);
                }
            });
    if (direction == Direction.REQUEST) {
        after.forEach(
                (name, spec) -> {
                    if (spec.required() && !before.containsKey(name)) {
                        problems.add("new required field: " + name);
                    }
                });
    }
    return problems;
}

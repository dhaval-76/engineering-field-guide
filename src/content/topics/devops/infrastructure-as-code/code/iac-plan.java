record Action(String kind, String name) {} // kind: create, update, delete

/**
 * The heart of every declarative tool (Terraform plan, Kubernetes controllers): compare what
 * the code declares with what exists, and list only the differences. Running it again on an
 * already-matching world produces nothing to do.
 */
List<Action> plan(Map<String, Map<String, String>> desired,
                  Map<String, Map<String, String>> actual) {
    List<Action> actions = new ArrayList<>();
    for (var want : new TreeMap<>(desired).entrySet()) {
        Map<String, String> have = actual.get(want.getKey());
        if (have == null) actions.add(new Action("create", want.getKey()));
        else if (!have.equals(want.getValue())) actions.add(new Action("update", want.getKey()));
    }
    for (String name : new TreeSet<>(actual.keySet())) {
        // exists but no longer declared, or changed by hand and not in code: drift
        if (!desired.containsKey(name)) actions.add(new Action("delete", name));
    }
    return actions;
}

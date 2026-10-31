/** Merge two edited versions of a document made of blocks (paragraphs) with stable ids. */
static MergeResult merge(Map<String, String> base, Map<String, String> ours, Map<String, String> theirs) {
    Map<String, String> merged = new LinkedHashMap<>();
    List<Conflict> conflicts = new ArrayList<>();
    Set<String> ids = new LinkedHashSet<>();
    ids.addAll(base.keySet()); ids.addAll(ours.keySet()); ids.addAll(theirs.keySet());

    for (String id : ids) {
        // null = absent / deleted
        String b = base.get(id), o = ours.get(id), t = theirs.get(id);
        // same on both sides
        if (Objects.equals(o, t)) { if (o != null) merged.put(id, o); }
        // only theirs changed
        else if (Objects.equals(o, b)) { if (t != null) merged.put(id, t); }
        // only ours changed
        else if (Objects.equals(t, b)) { if (o != null) merged.put(id, o); }
        // both changed differently
        else conflicts.add(new Conflict(id, b, o, t));
    }
    // order: real implementations also merge block ORDER
    return new MergeResult(merged, conflicts);
}
record Conflict(String blockId, String base, String ours, String theirs) {}
record MergeResult(Map<String, String> blocks, List<Conflict> conflicts) {}

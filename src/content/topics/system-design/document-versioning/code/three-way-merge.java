/** Merge two edited versions of a document made of blocks (paragraphs) with stable ids. */
static MergeResult merge(Map<String, String> base, Map<String, String> ours, Map<String, String> theirs) {
    Map<String, String> merged = new LinkedHashMap<>();
    List<Conflict> conflicts = new ArrayList<>();
    Set<String> ids = new LinkedHashSet<>();
    ids.addAll(base.keySet()); ids.addAll(ours.keySet()); ids.addAll(theirs.keySet());

    for (String id : ids) {
        String b = base.get(id), o = ours.get(id), t = theirs.get(id);    // null = absent / deleted
        if (Objects.equals(o, t)) { if (o != null) merged.put(id, o); }   // same on both sides
        else if (Objects.equals(o, b)) { if (t != null) merged.put(id, t); }   // only theirs changed
        else if (Objects.equals(t, b)) { if (o != null) merged.put(id, o); }   // only ours changed
        else conflicts.add(new Conflict(id, b, o, t));                   // both changed differently
    }
    return new MergeResult(merged, conflicts);   // order: real implementations also merge block ORDER
}
record Conflict(String blockId, String base, String ours, String theirs) {}
record MergeResult(Map<String, String> blocks, List<Conflict> conflicts) {}

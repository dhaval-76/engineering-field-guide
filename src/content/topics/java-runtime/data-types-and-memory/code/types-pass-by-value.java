// Java is ALWAYS pass-by-value. For objects, the value passed is a copy of the reference.
// caller unaffected
static void reassign(List<String> list) { list = new ArrayList<>(); list.add("x"); }
// caller sees "y"
static void mutate(List<String> list)   { list.add("y"); }

List<String> mine = new ArrayList<>();
reassign(mine);   // mine is still []
mutate(mine);     // mine is now ["y"] — both references point at the same object

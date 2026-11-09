List<String> removeInvalidParentheses(String s) {
    Set<String> seen = new HashSet<>(List.of(s));
    List<String> level = List.of(s);
    while (true) { // BFS: each level removes one more character
        List<String> valid = level.stream().filter(this::balanced).toList();
        if (!valid.isEmpty()) return valid;
        List<String> next = new ArrayList<>();
        for (String t : level) {
            for (int i = 0; i < t.length(); i++) {
                if (t.charAt(i) != '(' && t.charAt(i) != ')') continue;
                String shorter = t.substring(0, i) + t.substring(i + 1);
                if (seen.add(shorter)) next.add(shorter);
            }
        }
        level = next;
    }
}

boolean balanced(String t) {
    int open = 0;
    for (char c : t.toCharArray()) {
        if (c == '(') open++;
        else if (c == ')' && --open < 0) return false;
    }
    return open == 0;
}

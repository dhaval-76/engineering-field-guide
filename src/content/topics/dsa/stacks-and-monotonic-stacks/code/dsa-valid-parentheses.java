boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();
    Map<Character, Character> openerFor = Map.of(')', '(', ']', '[', '}', '{');
    for (char c : s.toCharArray()) {
        if (openerFor.containsKey(c)) {
            if (stack.isEmpty() || stack.pop() != openerFor.get(c)) return false;
        } else {
            stack.push(c);
        }
    }
    return stack.isEmpty();                     // leftovers are unclosed openers
}

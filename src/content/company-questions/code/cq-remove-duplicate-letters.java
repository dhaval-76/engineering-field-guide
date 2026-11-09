String removeDuplicateLetters(String s) {
    int[] last = new int[26];
    for (int i = 0; i < s.length(); i++) last[s.charAt(i) - 'a'] = i;
    boolean[] used = new boolean[26];
    StringBuilder stack = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (used[c - 'a']) continue;
        // Drop a bigger letter if it appears again later.
        while (stack.length() > 0) {
            char top = stack.charAt(stack.length() - 1);
            if (top < c || last[top - 'a'] < i) break;
            used[top - 'a'] = false;
            stack.setLength(stack.length() - 1);
        }
        stack.append(c);
        used[c - 'a'] = true;
    }
    return stack.toString();
}

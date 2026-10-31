// "3[a2[c]]" → "accaccacc". Each '[' saves the outer state; each ']' finishes a level.
String decodeString(String s) {
    Deque<Integer> counts = new ArrayDeque<>();
    Deque<StringBuilder> outer = new ArrayDeque<>();
    StringBuilder current = new StringBuilder();
    int k = 0;
    for (char c : s.toCharArray()) {
        if (Character.isDigit(c)) {
            k = k * 10 + (c - '0');                  // counts can have several digits
        } else if (c == '[') {
            counts.push(k);
            outer.push(current);                     // remember the text before '['
            current = new StringBuilder();
            k = 0;
        } else if (c == ']') {
            String inner = current.toString().repeat(counts.pop());
            current = outer.pop().append(inner);     // back to the enclosing level
        } else {
            current.append(c);
        }
    }
    return current.toString();
}
// "3[a]2[bc]" → "aaabcbc", "2[abc]3[cd]ef" → "abcabccdcdcdef". O(output length).

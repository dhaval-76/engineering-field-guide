int calculate(String s) {
    Deque<Integer> signs = new ArrayDeque<>();
    signs.push(1); // sign applied to the current bracket
    int result = 0, sign = 1;
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (Character.isDigit(c)) {
            int value = 0;
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                value = value * 10 + s.charAt(i++) - '0';
            }
            i--;
            result += sign * value;
        } else if (c == '+') sign = signs.peek();
        else if (c == '-') sign = -signs.peek();
        else if (c == '(') signs.push(sign);
        else if (c == ')') signs.pop();
    }
    return result;
}

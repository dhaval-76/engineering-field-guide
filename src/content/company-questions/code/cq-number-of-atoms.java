String countOfAtoms(String f) {
    Deque<Map<String, Integer>> stack = new ArrayDeque<>();
    stack.push(new TreeMap<>());
    for (int i = 0; i < f.length(); ) {
        char c = f.charAt(i);
        if (c == '(') {
            stack.push(new TreeMap<>());
            i++;
        } else if (c == ')') {
            int[] end = {i + 1};
            int times = readCount(f, end);
            i = end[0];
            Map<String, Integer> group = stack.pop();
            group.forEach((atom, n) -> stack.peek().merge(atom, n * times, Integer::sum));
        } else {
            int j = i + 1;
            while (j < f.length() && Character.isLowerCase(f.charAt(j))) j++;
            String atom = f.substring(i, j);
            int[] end = {j};
            stack.peek().merge(atom, readCount(f, end), Integer::sum);
            i = end[0];
        }
    }
    StringBuilder out = new StringBuilder();
    stack.peek().forEach((atom, n) -> out.append(atom).append(n > 1 ? n : ""));
    return out.toString();
}

int readCount(String f, int[] at) {
    int start = at[0];
    while (at[0] < f.length() && Character.isDigit(f.charAt(at[0]))) at[0]++;
    return at[0] == start ? 1 : Integer.parseInt(f.substring(start, at[0]));
}

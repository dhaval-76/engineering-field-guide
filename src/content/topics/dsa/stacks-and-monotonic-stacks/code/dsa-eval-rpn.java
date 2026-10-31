// Reverse Polish notation: operands first, then the operator. ["2","1","+","3","*"] =
// (2+1)*3.
int evalRPN(String[] tokens) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (String t : tokens) {
        switch (t) {
            case "+", "-", "*", "/" -> {
                int b = stack.pop(), a = stack.pop();   // order matters for - and /
                stack.push(switch (t) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    default -> a / b;                     // truncates toward zero
                });
            }
            default -> stack.push(Integer.parseInt(t));
        }
    }
    return stack.pop();
}
// ["4","13","5","/","+"] → 6. O(n). This stack machine is how compilers and the JVM
// evaluate.

int[] dailyTemperatures(int[] temps) {
    int[] answer = new int[temps.length];             // default 0: no warmer day
    // indices; temps decreasing bottom → top
    Deque<Integer> waiting = new ArrayDeque<>();
    for (int i = 0; i < temps.length; i++) {
        while (!waiting.isEmpty() && temps[i] > temps[waiting.peek()]) {
            int j = waiting.pop();
            answer[j] = i - j;                        // day i is j's next warmer day
        }
        waiting.push(i);
    }
    return answer;
}
// [73,74,75,71,69,72,76,73] → [1,1,4,2,1,1,0,0]

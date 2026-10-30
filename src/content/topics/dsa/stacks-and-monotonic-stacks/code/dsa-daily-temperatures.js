function dailyTemperatures(temps) {
  const answer = new Array(temps.length).fill(0);
  const waiting = [];
  temps.forEach((t, i) => {
    while (waiting.length && t > temps[waiting.at(-1)]) {
      const j = waiting.pop();
      answer[j] = i - j;
    }
    waiting.push(i);
  });
  return answer;
}

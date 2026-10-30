function characterReplacement(s, k) {
  const count = new Array(26).fill(0);
  let left = 0, maxFreq = 0, best = 0;
  for (let right = 0; right < s.length; right++) {
    maxFreq = Math.max(maxFreq, ++count[s.charCodeAt(right) - 65]);
    while (right - left + 1 - maxFreq > k) count[s.charCodeAt(left++) - 65]--;
    best = Math.max(best, right - left + 1);
  }
  return best;
}

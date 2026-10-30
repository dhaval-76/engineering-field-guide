const fibNaive = (n) => (n < 2 ? n : fibNaive(n - 1) + fibNaive(n - 2));   // O(2^n)

function fibMemo(n, memo = new Map()) {                                    // O(n)
  if (n < 2) return n;
  if (!memo.has(n)) memo.set(n, fibMemo(n - 1, memo) + fibMemo(n - 2, memo));
  return memo.get(n);
}

function fib(n) {                                                          // O(n), O(1) space
  let [prev, cur] = [0n, 1n];                                              // BigInt: no overflow
  for (let i = 0; i < n; i++) [prev, cur] = [cur, prev + cur];
  return prev;
}

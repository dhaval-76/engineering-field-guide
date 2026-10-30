const singleNumber = (nums) => nums.reduce((x, n) => x ^ n, 0);

function missingNumber(nums) {
  let x = nums.length;
  nums.forEach((n, i) => (x ^= i ^ n));
  return x;
}

function popcount(x) {
  let count = 0;
  x >>>= 0;                       // treat as unsigned 32-bit
  while (x) { x &= x - 1; count++; }
  return count;
}

const isPowerOfTwo = (x) => x > 0 && (x & (x - 1)) === 0;   // valid up to 2^30 (32-bit ops)

// JS has one number type: IEEE-754 double. Integers are exact only up to 2^53 − 1.
0.1 + 0.2 === 0.3;                     // false
Number.MAX_SAFE_INTEGER + 2;           // 9007199254740992 — precision lost
9007199254740993n + 1n;                // BigInt for exact large integers (can't mix with Number)

// Primitive vs object wrappers exist but are rarely used:
typeof 5;                              // "number"
typeof new Number(5);                  // "object" — avoid; === compares references

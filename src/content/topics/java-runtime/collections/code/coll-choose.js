// JavaScript's built-ins map onto the same needs:
const history = [];                 // Array: ordered, indexed
const seenIds = new Set();          // Set: membership, insertion-ordered
const byEmail = new Map();          // Map: any key type, insertion-ordered
// No built-in sorted map or priority queue: keep an array sorted (binary-search inserts)
// for small sizes, or use a library heap/tree for large ones.
const queue = [];                   // push/shift works, but shift() is O(n) — use an index or a deque lib for big queues

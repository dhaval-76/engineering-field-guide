// JS functions are first-class: no functional-interface ceremony.
const byAge = (a, b) => a.age - b.age;
const isPaid = (o) => o.status === 'PAID';
const compose = (...fns) => (x) => fns.reduce((v, f) => f(v), x);
const normalise = compose((s) => s.trim(), (s) => s.toLowerCase());

// Closures capture variables by reference (not a copy), so later changes are visible:
let threshold = 10;
const small = (x) => x < threshold;
threshold = 20;           // small(15) is now true

// 1. Wrapper caching makes == work by accident for small values
Integer a = 127, b = 127;   a == b;   // true  (cached)
Integer c = 128, d = 128;   c == d;   // false (two objects) — use c.equals(d)

// 2. Unboxing null throws
Integer maybe = cache.get(key);         // may be null
int total = maybe + 1;                  // NullPointerException at the hidden maybe.intValue()

// 3. Boxing in hot loops creates garbage
Long sum = 0L;
for (long i = 0; i < 1_000_000; i++) sum += i;   // ~1M Long objects allocated
long fast = 0L;
for (long i = 0; i < 1_000_000; i++) fast += i;  // zero allocations

// 4. Arithmetic surprises
int big = Integer.MAX_VALUE;  big + 1;           // -2147483648 (silent overflow) — Math.addExact throws instead
0.1 + 0.2 == 0.3;                                 // false — binary floating point; use BigDecimal or integer cents for money
(int) 3.99;                                       // 3 — casts truncate toward zero
'a' + 1;                                          // 98 — char promotes to int

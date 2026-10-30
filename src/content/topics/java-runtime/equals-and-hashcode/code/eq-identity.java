String a = "hello";
String b = "hello";
String c = new String("hello");

a == b;          // true  — both refer to the same pooled literal
a == c;          // false — c is a different object on the heap
a.equals(c);     // true  — same characters

Integer x = 127, y = 127;
x == y;          // true  — Integer.valueOf caches -128..127
Integer p = 128, q = 128;
p == q;          // false — two different Integer objects; always use equals() for wrappers

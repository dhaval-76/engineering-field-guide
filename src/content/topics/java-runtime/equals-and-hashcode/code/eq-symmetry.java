// ✗ Symmetry broken by inheritance + instanceof
class Point { final int x, y; Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) { return o instanceof Point p && p.x == x && p.y == y; }
    @Override public int hashCode() { return Objects.hash(x, y); } }

class ColorPoint extends Point { final String color;
    ColorPoint(int x, int y, String c) { super(x, y); color = c; }
    @Override public boolean equals(Object o) { return o instanceof ColorPoint cp && super.equals(cp) && cp.color.equals(color); } }

Point p = new Point(1, 2);
ColorPoint cp = new ColorPoint(1, 2, "red");
p.equals(cp);   // true  (a ColorPoint is a Point)
// false (a Point is not a ColorPoint)  → symmetry violated; sets behave inconsistently
cp.equals(p);

// ✓ Options: make value classes final (or records), compare getClass() instead of
// instanceof,
//   or prefer composition: ColorPoint HAS-A Point.

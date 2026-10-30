class Circle {
  constructor(r) { this.r = r; }
  area() { return Math.PI * this.r ** 2; }
}
class Rectangle {
  constructor(w, h) { this.w = w; this.h = h; }
  area() { return this.w * this.h; }
}

// Any object with an area() method works here — no common base class required.
export const total = (shapes) => shapes.reduce((sum, s) => sum + s.area(), 0);

total([new Circle(1), new Rectangle(2, 3)]); // ≈ 9.14

// JS has no overloading: one function inspects its arguments if it must vary by type.

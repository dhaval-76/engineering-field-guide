// The classic Rectangle/Square trap: Square changes the meaning of setWidth().
class Rectangle {
  setWidth(w) { this.w = w; }
  setHeight(h) { this.h = h; }
  area() { return this.w * this.h; }
}
class Square extends Rectangle {
  setWidth(w) { this.w = this.h = w; }   // also changes height!
  setHeight(h) { this.w = this.h = h; }
}

function stretch(rect) {
  rect.setWidth(5);
  rect.setHeight(4);
  return rect.area();                    // caller expects 20
}
stretch(new Rectangle()); // 20
stretch(new Square());    // 16 — substituting the subtype broke the caller

// Fix: don't make Square a mutable Rectangle. Use separate immutable shapes
// sharing a Shape contract that only promises area().

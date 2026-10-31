# The classic Rectangle/Square trap: Square changes the meaning of set_width().
class Rectangle:
    def set_width(self, w): self.w = w
    def set_height(self, h): self.h = h
    def area(self): return self.w * self.h

class Square(Rectangle):
    def set_width(self, w): self.w = self.h = w      # also changes height!
    def set_height(self, h): self.w = self.h = h

def stretch(rect: Rectangle) -> int:
    rect.set_width(5)
    rect.set_height(4)
    return rect.area()                                # caller expects 20

stretch(Rectangle())   # 20
stretch(Square())      # 16 — substituting the subtype broke the caller

# Fix: don't make Square a mutable Rectangle. Use separate immutable shapes
# (frozen dataclasses) sharing a Shape protocol that only promises area().

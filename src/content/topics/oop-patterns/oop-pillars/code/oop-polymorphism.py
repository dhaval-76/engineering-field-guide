from dataclasses import dataclass
from math import pi


@dataclass
class Circle:
    r: float

    def area(self) -> float:
        return pi * self.r**2


@dataclass
class Rectangle:
    w: float
    h: float

    def area(self) -> float:
        return self.w * self.h


def total(shapes) -> float:
    return sum(s.area() for s in shapes)  # duck typing: anything with area()

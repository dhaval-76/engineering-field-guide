# Python ints are arbitrary precision objects — no overflow, but no primitives either.
2**100                          # exact
0.1 + 0.2 == 0.3                # False — same IEEE-754 floats
from decimal import Decimal
Decimal("0.1") + Decimal("0.2") == Decimal("0.3")   # True

# Small ints (-5..256) are cached, so `is` can mislead just like Java's ==:
a, b = 256, 256;  a is b        # True (cached)
x = int("257"); y = int("257"); x is y   # False — use == for values

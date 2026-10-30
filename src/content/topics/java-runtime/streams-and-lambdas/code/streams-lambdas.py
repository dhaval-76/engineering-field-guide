from functools import reduce

by_age = lambda u: u.age
is_paid = lambda o: o.status == "PAID"
compose = lambda *fns: lambda x: reduce(lambda v, f: f(v), fns, x)
normalise = compose(str.strip, str.lower)

# Python closures are late-binding too:
threshold = 10
small = lambda x: x < threshold
threshold = 20   # small(15) -> True

git bisect start
git bisect bad                        # the current commit is broken
git bisect good v2.14.0               # this release was fine

# Let Git drive: the script exits 0 for good, 1–127 (except 125) for bad, 125 to skip
git bisect run ./mvnw -q -Dtest=CheckoutTotalsTest test

# Git prints: "a1b2c3d is the first bad commit"
git bisect reset                      # return to where you started

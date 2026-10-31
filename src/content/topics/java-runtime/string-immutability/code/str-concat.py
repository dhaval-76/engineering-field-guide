# Python strings are immutable; repeated += in a loop can be quadratic.
# linear: join computes the total size once
csv = "\n".join(r.to_csv() for r in rows)

# io.StringIO is the StringBuilder equivalent when pieces come from many places:
import io
buf = io.StringIO()
for r in rows:
    buf.write(r.to_csv())
    buf.write("\n")
csv = buf.getvalue()

# uses the Node / DirNode model above
import fnmatch
import re
import time
from typing import Callable

Filter = Callable[["Node"], bool]       # each find option is a predicate (Specification pattern)

def name(glob: str) -> Filter:
    rx = re.compile(fnmatch.translate(glob))
    return lambda n: bool(rx.match(n.name))

def is_dir(n) -> bool:
    return n.type == "dir"

def size_at_least(nbytes: int) -> Filter:
    return lambda n: n.type == "file" and n.size >= nbytes

def modified_within(seconds: float, clock=time.time) -> Filter:
    return lambda n: n.modified > clock() - seconds

def all_of(*fs: Filter) -> Filter:
    return lambda n: all(f(n) for f in fs)

def any_of(*fs: Filter) -> Filter:
    return lambda n: any(f(n) for f in fs)

def negate(f: Filter) -> Filter:
    return lambda n: not f(n)

def find(start, flt: Filter, max_depth: float = float("inf")) -> list:
    """Iterative DFS with an explicit stack — no recursion limit on deep trees."""
    out, stack = [], [(start, 0)]
    while stack:
        n, depth = stack.pop()
        if flt(n):
            out.append(n)
        if n.type == "dir" and depth < max_depth:
            stack.extend((c, depth + 1) for c in n.children)
    return out

# find /src -name "*.java" -size +1k ! -name "*Test*"
# hits = find(fs.resolve("/src"), all_of(name("*.java"), size_at_least(1024), negate(name("*Test*"))))

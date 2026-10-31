import operator

# Reverse Polish notation: operands first, then the operator. ["2","1","+","3","*"] =
# (2+1)*3.
OPS = {
    "+": operator.add,
    "-": operator.sub,
    "*": operator.mul,
    "/": lambda a, b: int(a / b),   # truncate toward zero (// would floor: -7 // 2 == -4)
}


def eval_rpn(tokens: list[str]) -> int:
    stack: list[int] = []
    for t in tokens:
        if t in OPS:
            b, a = stack.pop(), stack.pop()   # order matters for - and /
            stack.append(OPS[t](a, b))
        else:
            stack.append(int(t))
    return stack.pop()
# ["4","13","5","/","+"] → 6. O(n).

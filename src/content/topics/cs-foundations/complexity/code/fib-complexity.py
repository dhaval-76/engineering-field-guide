from functools import cache

def fib_naive(n):            # O(2^n)
    return n if n < 2 else fib_naive(n - 1) + fib_naive(n - 2)

@cache
def fib_memo(n):             # O(n) time and space
    return n if n < 2 else fib_memo(n - 1) + fib_memo(n - 2)

def fib(n):                  # O(n) time, O(1) space
    prev, cur = 0, 1
    for _ in range(n):
        prev, cur = cur, prev + cur
    return prev

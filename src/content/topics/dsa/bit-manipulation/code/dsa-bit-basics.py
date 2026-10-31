from functools import reduce
from operator import xor

def single_number(nums):
    return reduce(xor, nums, 0)

def missing_number(nums):
    return reduce(xor, range(len(nums) + 1), 0) ^ reduce(xor, nums, 0)

def count_bits(n):
    bits = [0] * (n + 1)
    for i in range(1, n + 1):
        bits[i] = bits[i >> 1] + (i & 1)
    return bits

def is_power_of_two(x):
    return x > 0 and x & (x - 1) == 0
# Python ints are unbounded: x.bit_count() (3.10+) counts set bits; negatives have
# infinite leading 1s.

import threading

def transfer(src, dst, amount):
    first, second = sorted((src, dst), key=lambda a: a.id)    # global lock order
    with first.lock, second.lock:
        src.balance -= amount
        dst.balance += amount

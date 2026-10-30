import random

class RandomizedSet:
    def __init__(self):
        self.index_of = {}
        self.values = []

    def insert(self, val) -> bool:
        if val in self.index_of:
            return False
        self.index_of[val] = len(self.values)
        self.values.append(val)
        return True

    def remove(self, val) -> bool:
        if val not in self.index_of:
            return False
        i, last = self.index_of.pop(val), self.values.pop()
        if i < len(self.values):
            self.values[i] = last
            self.index_of[last] = i
        return True

    def get_random(self):
        return random.choice(self.values)

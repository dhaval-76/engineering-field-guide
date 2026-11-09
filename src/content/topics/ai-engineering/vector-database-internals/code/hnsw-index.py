import heapq
import math
import random


class Hnsw:
    """A small HNSW index over vectors, using squared Euclidean distance."""

    def __init__(self, m=8, ef_construction=64, seed=None):
        self.m = m  # links per node on upper layers; layer 0 allows 2 * m
        self.ef_construction = ef_construction
        self.level_factor = 1 / math.log(m)
        self.random = random.Random(seed)
        self.vectors = []
        self.links = []  # node -> layer -> neighbours
        self.entry = None

    @staticmethod
    def distance(a, b):
        return sum((x - y) ** 2 for x, y in zip(a, b))

    def top_level(self, node):
        return len(self.links[node]) - 1

    def search_layer(self, query, start, ef, layer):
        """Best-first search of one layer: up to ef (dist, id) pairs, nearest first."""
        first = self.distance(query, self.vectors[start])
        visited = {start}
        frontier = [(first, start)]  # min-heap
        best = [(-first, start)]  # max-heap, via negated distance
        while frontier:
            dist, node = heapq.heappop(frontier)
            # Everything left to explore is further than the worst result we are keeping.
            if len(best) >= ef and dist > -best[0][0]:
                break
            for neighbour in self.links[node][layer]:
                if neighbour in visited:
                    continue
                visited.add(neighbour)
                d = self.distance(query, self.vectors[neighbour])
                if len(best) < ef or d < -best[0][0]:
                    heapq.heappush(frontier, (d, neighbour))
                    heapq.heappush(best, (-d, neighbour))
                    if len(best) > ef:
                        heapq.heappop(best)
        return sorted((-d, node) for d, node in best)

    def descend(self, query, target):
        """Walks down from the top layer to just above target, keeping the closest node."""
        node = self.entry
        for layer in range(self.top_level(self.entry), target, -1):
            node = self.search_layer(query, node, 1, layer)[0][1]
        return node

    def add(self, vector):
        node_id = len(self.vectors)
        # Most nodes stay on layer 0; each higher layer holds about 1/m of the one below.
        level = int(-math.log(1 - self.random.random()) * self.level_factor)
        self.vectors.append(vector)
        self.links.append([[] for _ in range(level + 1)])
        if self.entry is None:
            self.entry = node_id
            return node_id
        old_top = self.top_level(self.entry)
        node = self.descend(vector, level)
        for layer in range(min(level, old_top), -1, -1):
            near = self.search_layer(vector, node, self.ef_construction, layer)
            limit = 2 * self.m if layer == 0 else self.m
            for _, other in near[: self.m]:
                self.links[node_id][layer].append(other)
                self.links[other][layer].append(node_id)
                if len(self.links[other][layer]) > limit:
                    self.prune(other, layer, limit)
            node = near[0][1]
        if level > old_top:
            self.entry = node_id
        return node_id

    def prune(self, node, layer, limit):
        """Keeps a node's closest neighbours when it has gained too many links."""
        origin = self.vectors[node]
        self.links[node][layer].sort(key=lambda n: self.distance(origin, self.vectors[n]))
        del self.links[node][layer][limit:]

    def search(self, query, k, ef_search):
        if self.entry is None:
            return []
        start = self.descend(query, 0)
        return self.search_layer(query, start, max(ef_search, k), 0)[:k]

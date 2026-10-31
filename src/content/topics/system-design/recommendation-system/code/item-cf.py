import heapq
import math
from collections import defaultdict

def item_neighbours(interactions: dict[str, set[str]], top_n: int = 50) -> dict[str, list[tuple[float, str]]]:
    """Offline batch job: cosine similarity between items from co-occurrence in users' histories.
    sim(i, j) = |users of i ∩ users of j| / sqrt(|users of i| × |users of j|)."""
    users_of: dict[str, int] = defaultdict(int)
    co: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for items in interactions.values():
        for i in items:
            users_of[i] += 1
            for j in items:
                if i != j:
                    co[i][j] += 1
    return {i: heapq.nsmallest(top_n, ((-c / math.sqrt(users_of[i] * users_of[j]), j) for j, c in js.items()))
            # stored negated so nsmallest = most similar first
            for i, js in co.items()}

def recommend(history: set[str], neighbours: dict[str, list[tuple[float, str]]], k: int) -> list[str]:
    """Online: score unseen items by summed similarity to what the user engaged with."""
    score: dict[str, float] = defaultdict(float)
    for i in history:
        for neg_sim, j in neighbours.get(i, []):
            if j not in history:
                score[j] += -neg_sim
    return [j for _, j in sorted((-round(s, 12), j) for j, s in score.items())[:k]]

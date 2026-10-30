import math, re
from collections import defaultdict

STOP = {"the", "a", "an", "and", "of", "on", "to", "in"}
K1, B = 1.2, 0.75

def analyse(text: str) -> list[str]:
    terms = [t for t in re.split(r"[^\w]+", text.lower()) if t and t not in STOP]
    return [t[:-3] if t.endswith("ing") and len(t) > 5 else t.rstrip("s") if len(t) > 3 else t for t in terms]

class InvertedIndex:
    def __init__(self):
        self.postings: dict[str, dict[str, int]] = defaultdict(dict)   # term → {doc: tf}
        self.lengths: dict[str, int] = {}

    def index(self, doc_id: str, text: str) -> None:
        terms = analyse(text)
        self.lengths[doc_id] = len(terms)
        for t in terms:
            self.postings[t][doc_id] = self.postings[t].get(doc_id, 0) + 1

    def search(self, query: str, k: int = 10) -> list[tuple[str, float]]:
        n = len(self.lengths)
        avg = sum(self.lengths.values()) / max(1, n)
        scores: dict[str, float] = defaultdict(float)
        for term in analyse(query):
            docs = self.postings.get(term, {})
            idf = math.log(1 + (n - len(docs) + 0.5) / (len(docs) + 0.5))
            for doc, tf in docs.items():
                scores[doc] += idf * tf * (K1 + 1) / (tf + K1 * (1 - B + B * self.lengths[doc] / avg))
        return sorted(scores.items(), key=lambda kv: kv[1], reverse=True)[:k]

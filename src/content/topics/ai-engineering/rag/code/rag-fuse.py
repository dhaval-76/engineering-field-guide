from dataclasses import dataclass

@dataclass(frozen=True)
class Hit:
    chunk_id: str
    text: str

def fuse(vector_hits: list[Hit], keyword_hits: list[Hit], top_n: int, k: int = 60) -> list[Hit]:
    """Reciprocal rank fusion: score = sum of 1 / (k + rank)."""
    score: dict[str, float] = {}
    by_id: dict[str, Hit] = {}
    for hits in (vector_hits, keyword_hits):
        for rank, h in enumerate(hits, start=1):
            score[h.chunk_id] = score.get(h.chunk_id, 0.0) + 1 / (k + rank)
            by_id.setdefault(h.chunk_id, h)
    best = sorted(score, key=score.get, reverse=True)[:top_n]
    return [by_id[i] for i in best]

def build_prompt(question: str, chunks: list[Hit]) -> str:
    sources = "\n".join(f'<source id="{h.chunk_id}">\n{h.text}\n</source>' for h in chunks)
    return ("Answer the question using only the sources below.\n"
            "Cite sources like [id]. If the sources don't contain the answer, say you don't know.\n\n"
            f"{sources}\n\nQuestion: {question}")

import re

def chunk(text: str, max_words: int, overlap: int) -> list[str]:
    """Split text into ~max_words-word chunks on paragraph boundaries, carrying `overlap` words forward."""
    chunks: list[str] = []
    current: list[str] = []
    for para in re.split(r"\n\s*\n", text):
        words = para.split()
        step = max_words - overlap  # overlap must be < max_words
        for i in range(0, len(words), step):
            piece = words[i:i + step]
            if current and len(current) + len(piece) > max_words:
                chunks.append(" ".join(current))
                current = current[-overlap:] if overlap else []
            current.extend(piece)
    if current:
        chunks.append(" ".join(current))
    return chunks

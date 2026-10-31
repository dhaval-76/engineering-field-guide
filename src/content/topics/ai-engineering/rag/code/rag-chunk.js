// Split text into ~maxWords-word chunks on paragraph boundaries, carrying `overlap` words
// forward.
export function chunk(text, maxWords, overlap) {
  const chunks = [];
  let current = [];
  for (const para of text.split(/\n\s*\n/)) {
    const words = para.trim().split(/\s+/).filter(Boolean);
    const step = maxWords - overlap; // overlap must be < maxWords
    for (let i = 0; i < words.length; i += step) {
      const piece = words.slice(i, i + step);
      if (current.length && current.length + piece.length > maxWords) {
        chunks.push(current.join(' '));
        current = current.slice(Math.max(0, current.length - overlap));
      }
      current.push(...piece);
    }
  }
  if (current.length) chunks.push(current.join(' '));
  return chunks;
}

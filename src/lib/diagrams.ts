/** Diagram SVGs (hand-drawn or rendered from .mmd by `npm run diagrams`), keyed by file name. */
const files = import.meta.glob('/src/content/topics/**/diagrams/*.svg', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

const index = new Map<string, string>();
for (const [path, svg] of Object.entries(files)) {
  const id = path
    .split('/')
    .pop()!
    .replace(/\.svg$/, '');
  if (index.has(id)) throw new Error(`Duplicate diagram id "${id}": ${path}`);
  index.set(id, svg);
}

export function getDiagram(id: string): string {
  const svg = index.get(id);
  if (!svg)
    throw new Error(
      `Diagram "${id}" not found. Add diagrams/${id}.svg or diagrams/${id}.mmd and run npm run diagrams.`,
    );
  return svg;
}

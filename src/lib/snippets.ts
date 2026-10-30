import { codeLanguages, type CodeLanguageId } from '~/site.config';

/** Every code file under a topic's `code/` folder, keyed by path. Loaded as raw text at build. */
const files = import.meta.glob('/src/content/topics/**/code/*', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

export interface SnippetVariant {
  lang: CodeLanguageId;
  label: string;
  code: string;
  path: string;
}

const byExt = new Map<string, (typeof codeLanguages)[number]>(codeLanguages.map((l) => [l.ext, l]));

/** id → variants, built once. Snippet ids are global so MDX can reference them without paths. */
const index = new Map<string, SnippetVariant[]>();
for (const [path, code] of Object.entries(files)) {
  const name = path.split('/').pop()!;
  const dot = name.lastIndexOf('.');
  const id = name.slice(0, dot);
  const language = byExt.get(name.slice(dot + 1));
  if (!language)
    throw new Error(
      `Unsupported snippet extension: ${path}. Add it to codeLanguages in site.config.ts.`,
    );
  const list = index.get(id) ?? [];
  const clash = list.find((v) => v.lang === language.id);
  if (clash)
    throw new Error(
      `Duplicate snippet id "${id}" for ${language.label}: ${clash.path} and ${path}`,
    );
  list.push({ lang: language.id, label: language.label, code, path });
  index.set(id, list);
}

const order = new Map(codeLanguages.map((l, i) => [l.id, i]));

export function getSnippet(id: string): SnippetVariant[] {
  const variants = index.get(id);
  if (!variants?.length)
    throw new Error(`No code files found for snippet id "${id}" (expected code/${id}.<ext>).`);
  return [...variants].sort((a, b) => order.get(a.lang)! - order.get(b.lang)!);
}

export function allSnippetIds(): string[] {
  return [...index.keys()];
}

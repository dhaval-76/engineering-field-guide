import { codeLanguages, type CodeLanguageId } from '~/site.config';

/** Every code file under a topic's `code/` folder, keyed by path. Loaded as raw text at build. */
const files = import.meta.glob('/src/content/topics/**/code/*', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

export interface SnippetVariant {
  /** Language used for highlighting. */
  lang: CodeLanguageId;
  /** Tab key, matched against the reader's preferred language. Same as `lang` unless the file is a variant. */
  tab: CodeLanguageId;
  label: string;
  code: string;
  path: string;
}

const byExt = new Map<string, (typeof codeLanguages)[number]>(codeLanguages.map((l) => [l.ext, l]));

/**
 * id → variants, built once. Snippet ids are global so MDX can reference them without paths.
 * `<id>.<ext>` is one tab per language. `<id>@<appExt>.<ext>` lets one snippet hold several files
 * of the same language for different stacks (e.g. dockerfile@java.dockerfile, dockerfile@py.dockerfile):
 * the tab is labelled and preference-matched by the app language.
 */
const index = new Map<string, SnippetVariant[]>();
for (const [path, code] of Object.entries(files)) {
  const name = path.split('/').pop()!;
  const dot = name.lastIndexOf('.');
  const [id, variant] = name.slice(0, dot).split('@') as [string, string | undefined];
  const language = byExt.get(name.slice(dot + 1));
  if (!language)
    throw new Error(
      `Unsupported snippet extension: ${path}. Add it to codeLanguages in site.config.ts.`,
    );
  const app = variant === undefined ? language : byExt.get(variant);
  if (!app) throw new Error(`Unknown variant "@${variant}" in ${path}: use a language extension.`);
  const list = index.get(id) ?? [];
  const clash = list.find((v) => v.tab === app.id);
  if (clash)
    throw new Error(`Duplicate snippet id "${id}" for ${app.label}: ${clash.path} and ${path}`);
  list.push({ lang: language.id, tab: app.id, label: app.label, code, path });
  index.set(id, list);
}

const order = new Map(codeLanguages.map((l, i) => [l.id, i]));

export function getSnippet(id: string): SnippetVariant[] {
  const variants = index.get(id);
  if (!variants?.length)
    throw new Error(`No code files found for snippet id "${id}" (expected code/${id}.<ext>).`);
  return [...variants].sort((a, b) => order.get(a.tab)! - order.get(b.tab)!);
}

export function allSnippetIds(): string[] {
  return [...index.keys()];
}

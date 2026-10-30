import { createHighlighter, type Highlighter } from 'shiki';

const langs = [
  'java',
  'javascript',
  'typescript',
  'python',
  'go',
  'sql',
  'bash',
  'yaml',
  'json',
  'http',
  'text',
  'properties',
  'dockerfile',
  'xml',
];
let highlighter: Promise<Highlighter> | undefined;

export async function highlight(code: string, lang: string): Promise<string> {
  highlighter ??= createHighlighter({ themes: ['github-light', 'github-dark-dimmed'], langs });
  const h = await highlighter;
  const known = h.getLoadedLanguages().includes(lang) ? lang : 'text';
  return h.codeToHtml(code.replace(/\n$/, ''), {
    lang: known,
    themes: { light: 'github-light', dark: 'github-dark-dimmed' },
    defaultColor: false,
  });
}

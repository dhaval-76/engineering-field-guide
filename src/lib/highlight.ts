import { createHighlighter, type Highlighter } from 'shiki';
import { CONTRAST_FIXES, indentTransformer } from './code-contrast';

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
  'proto',
  'graphql',
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
    colorReplacements: CONTRAST_FIXES as unknown as Record<string, Record<string, string>>,
    transformers: [indentTransformer],
  });
}

import { Marked } from 'marked';
import { highlight } from './highlight';

/** Renders Markdown strings from YAML data (Q&A answers, captions) at build time. */
const marked = new Marked({ async: true, gfm: true });

marked.use({
  async: true,
  async walkTokens(token) {
    if (token.type === 'code') {
      const t = token as typeof token & { escaped?: boolean; html?: string };
      t.html = await highlight(token.text, token.lang || 'text');
    }
  },
  renderer: {
    code(token) {
      const t = token as typeof token & { html?: string };
      return t.html ?? `<pre><code>${token.text}</code></pre>`;
    },
  },
});

export async function md(source: string): Promise<string> {
  return marked.parse(source);
}

export async function mdInline(source: string): Promise<string> {
  return marked.parseInline(source);
}

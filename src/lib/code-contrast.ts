// A few github-light / github-dark-dimmed colours fall below WCAG AA (4.5:1) on our code backgrounds.
// We darken (light theme) or lighten (dark theme) just those.
export const CONTRAST_FIXES = {
  'github-light': {
    '#6a737d': '#5f666e', // comments
    '#22863a': '#1a7330', // tags, inserted
    '#d73a49': '#bf3a47', // keywords
    '#e36209': '#a84b00', // parameters, attributes
  },
  'github-dark-dimmed': {
    '#768390': '#8394a3', // comments
  },
} as const;

/** For Markdown/MDX fenced code: rewrite the per-theme CSS variables Shiki emits. */
export const contrastTransformer = {
  name: 'code-contrast',
  postprocess(html: string) {
    let out = html;
    for (const [from, to] of Object.entries(CONTRAST_FIXES['github-light']))
      out = out.replace(new RegExp(`--shiki-light:${from}`, 'gi'), `--shiki-light:${to}`);
    for (const [from, to] of Object.entries(CONTRAST_FIXES['github-dark-dimmed']))
      out = out.replace(new RegExp(`--shiki-dark:${from}`, 'gi'), `--shiki-dark:${to}`);
    return out;
  },
};

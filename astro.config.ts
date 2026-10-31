import { contrastTransformer, indentTransformer } from './src/lib/code-contrast';
import { defineConfig } from 'astro/config';
import mdx from '@astrojs/mdx';
import react from '@astrojs/react';
import sitemap from '@astrojs/sitemap';
import { site } from './src/site.config';

export default defineConfig({
  site: site.origin,
  base: site.base,
  trailingSlash: 'ignore',
  // ~13 KB of CSS in total: inlining it removes two render-blocking requests on every page.
  build: { inlineStylesheets: 'always' },
  integrations: [mdx(), react(), sitemap()],
  markdown: {
    shikiConfig: {
      themes: { light: 'github-light', dark: 'github-dark-dimmed' },
      defaultColor: false,
      transformers: [contrastTransformer, indentTransformer],
    },
  },
});

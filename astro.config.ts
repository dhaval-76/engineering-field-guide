import { defineConfig } from 'astro/config';
import mdx from '@astrojs/mdx';
import react from '@astrojs/react';
import sitemap from '@astrojs/sitemap';
import { site } from './src/site.config';

export default defineConfig({
  site: site.origin,
  base: site.base,
  trailingSlash: 'ignore',
  integrations: [mdx(), react(), sitemap()],
  markdown: {
    shikiConfig: {
      themes: { light: 'github-light', dark: 'github-dark-dimmed' },
      defaultColor: false,
    },
  },
});

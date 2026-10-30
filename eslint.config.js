import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import astro from 'eslint-plugin-astro';

export default [
  { ignores: ['dist/', '.astro/', '.cache/', 'legacy/', 'drafts/', 'node_modules/', 'src/content/**/code/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...astro.configs.recommended,
  { languageOptions: { globals: { window: 'readonly', document: 'readonly', localStorage: 'readonly', console: 'readonly', process: 'readonly' } } },
];

/** Single place for site identity. Change `repo` if the GitHub repository is renamed. */
const repo = 'engineering-field-guide';
const githubUser = process.env.GITHUB_REPOSITORY_OWNER ?? 'dhaval-76';

export const site = {
  name: 'Engineering Field Guide',
  tagline:
    'Software engineering concepts, explained visually — for daily reference and interview prep.',
  origin: `https://${githubUser}.github.io`,
  base: `/${repo}`,
  repoUrl: `https://github.com/${githubUser}/${repo}`,
} as const;

/** Code languages in tab order. Add a language here and drop `<id>.<ext>` files next to existing snippets. */
export const codeLanguages = [
  { id: 'java', label: 'Java', ext: 'java' },
  { id: 'javascript', label: 'JavaScript', ext: 'js' },
  { id: 'python', label: 'Python', ext: 'py' },
  { id: 'typescript', label: 'TypeScript', ext: 'ts' },
  { id: 'go', label: 'Go', ext: 'go' },
  { id: 'sql', label: 'SQL', ext: 'sql' },
  { id: 'bash', label: 'Shell', ext: 'sh' },
  { id: 'yaml', label: 'YAML', ext: 'yaml' },
  { id: 'json', label: 'JSON', ext: 'json' },
  { id: 'dockerfile', label: 'Dockerfile', ext: 'dockerfile' },
  { id: 'properties', label: 'Properties', ext: 'properties' },
  { id: 'xml', label: 'XML', ext: 'xml' },
  { id: 'http', label: 'HTTP', ext: 'http' },
  { id: 'text', label: 'Text', ext: 'txt' },
] as const;

export type CodeLanguageId = (typeof codeLanguages)[number]['id'];
export const defaultLanguage: CodeLanguageId = 'java';

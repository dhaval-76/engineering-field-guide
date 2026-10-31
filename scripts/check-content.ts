/**
 * Structural checks Zod can't express on its own:
 *  - every <CodeTabs id>, <Diagram id>, <Walkthrough id>, <Term id> and solutionCode resolves
 *  - no orphan code/diagram/walkthrough files
 *  - snippet ids are globally unique per language
 *  - every askedAt entry has a source URL (also enforced by the schema)
 *  - no leftover level/schedule framing in content
 *  - every track has an Open Graph card in public/og/
 */
import { readFile } from 'node:fs/promises';
import { glob } from 'node:fs/promises';
import { basename, join } from 'node:path';
import { parse } from 'yaml';
import { posix } from 'node:path';

const root = new URL('../', import.meta.url).pathname;
const errors: string[] = [];
const warn: string[] = [];

async function list(pattern: string) {
  const out: string[] = [];
  for await (const f of glob(pattern, { cwd: root })) out.push(f);
  return out.sort();
}

const codeFiles = await list('src/content/topics/**/code/*');
const diagramFiles = await list('src/content/topics/**/diagrams/*.{svg,mmd}');
const walkFiles = await list('src/content/topics/**/walkthroughs/*.yaml');
const glossaryFiles = await list('src/content/glossary/*.yaml');
const mdxFiles = await list('src/content/topics/**/index.mdx');
const qaFiles = await list('src/content/topics/**/qa.yaml');

const stem = (f: string) => basename(f).replace(/\.[^.]+$/, '');
const snippetIds = new Map<string, string[]>();
for (const f of codeFiles) {
  const id = stem(f).split('@')[0]!; // <id>@<appExt>.<ext> is a per-stack variant of <id>
  snippetIds.set(id, [...(snippetIds.get(id) ?? []), f]);
}
// Snippets that demonstrate one language's own runtime (the JS event loop, ES modules) and have no Java equivalent.
const LANGUAGE_SPECIFIC = new Set(['singleton-module', 'event-loop-order', 'blocking-the-loop', 'resilience-other']);
for (const [id, files] of snippetIds) {
  // A variant's language is its @app extension (dockerfile@py.dockerfile → py).
  const exts = files.map((f) => basename(f).match(/@([^.]+)\./)?.[1] ?? f.split('.').pop());
  if (new Set(exts).size !== exts.length)
    errors.push(`Snippet "${id}" has two files with the same language: ${files.join(', ')}`);
  // SQL/shell/config-only snippets are language-neutral; only flag code that has no Java variant.
  const programmable = exts.some((e) => ['js', 'py', 'ts', 'go'].includes(e!));
  if (programmable && !exts.includes('java') && !LANGUAGE_SPECIFIC.has(id))
    warn.push(`Snippet "${id}" has no Java version (Java is the primary language).`);
}
const diagramIds = new Set(diagramFiles.map(stem));
const walkIds = new Set(walkFiles.map(stem));
// Diagram and walkthrough ids are global: two files with the same name would silently shadow each other.
for (const [kind, files] of [['diagram', diagramFiles.filter((f) => f.endsWith('.svg') || !diagramFiles.includes(f.replace(/\.mmd$/, '.svg')))], ['walkthrough', walkFiles]] as const) {
  const seen = new Map<string, string>();
  for (const f of files) {
    const id = stem(f);
    const prev = seen.get(id);
    if (prev) errors.push(`Duplicate ${kind} id "${id}": ${prev} and ${f}`);
    else seen.set(id, f);
  }
}
const termIds = new Set(glossaryFiles.map(stem));

const used = { code: new Set<string>(), diagram: new Set<string>(), walk: new Set<string>() };
const banned = /\bSDE[- ]?(1|2|3|I{1,3})\b|\bweek\s+\d\b|\bW[1-8]\b(?!\w)/i;

const topicKey = (f: string) => f.match(/topics\/([^/]+\/[^/]+)\/index\.mdx$/)?.[1];
const topicKeys = new Set(mdxFiles.map(topicKey).filter(Boolean));

for (const f of mdxFiles) {
  const src = await readFile(join(root, f), 'utf8');
  const self = topicKey(f);
  // Relative markdown links between topics, e.g. [RAG](../rag/) or [Kafka](../../system-design/kafka-deep-dive/)
  for (const m of src.matchAll(/\]\((\.\.\/[^)#\s]*)/g)) {
    if (!self) break;
    const target = posix.join(`/${self}/`, m[1]!).replace(/^\/|\/$/g, '');
    if (target.split('/').length === 2 && !topicKeys.has(target))
      errors.push(`${f}: link ${m[1]} points to missing topic ${target}`);
  }
  try {
    const fm = parse(src.split(/^---$/m)[1] ?? '') as { summary?: string };
    if ((fm.summary?.length ?? 0) > 240) errors.push(`${f}: summary is ${fm.summary!.length} chars (max 240)`);
  } catch (e) {
    errors.push(`${f}: invalid frontmatter YAML — ${(e as Error).message.split('\n')[0]}`);
  }
  const seen = new Set<string>();
  for (const m of src.matchAll(/<(?:CodeTabs|Diagram|Walkthrough)\s+id="([^"]+)"/g)) {
    if (seen.has(m[0]))
      errors.push(`${f}: ${m[0]}> is used twice on one page (duplicate HTML ids)`);
    seen.add(m[0]);
  }
  for (const m of src.matchAll(/<CodeTabs\s+id="([^"]+)"/g)) {
    used.code.add(m[1]!);
    if (!snippetIds.has(m[1]!)) errors.push(`${f}: <CodeTabs id="${m[1]}"> has no code files`);
  }
  for (const m of src.matchAll(/<Diagram\s+id="([^"]+)"/g)) {
    used.diagram.add(m[1]!);
    if (!diagramIds.has(m[1]!)) errors.push(`${f}: <Diagram id="${m[1]}"> not found`);
  }
  for (const m of src.matchAll(/<Walkthrough\s+id="([^"]+)"/g)) {
    used.walk.add(m[1]!);
    if (!walkIds.has(m[1]!)) errors.push(`${f}: <Walkthrough id="${m[1]}"> not found`);
  }
  for (const m of src.matchAll(/<Term\s+id="([^"]+)"/g)) {
    if (!termIds.has(m[1]!)) errors.push(`${f}: <Term id="${m[1]}"> has no glossary entry`);
  }
  const b = src.match(banned);
  if (b) errors.push(`${f}: contains level/schedule framing "${b[0]}"`);
}

for (const f of qaFiles) {
  const data = parse(await readFile(join(root, f), 'utf8')) ?? {};
  for (const p of data.practice ?? []) {
    if (p.solutionCode) {
      used.code.add(p.solutionCode);
      if (!snippetIds.has(p.solutionCode))
        errors.push(`${f}: solutionCode "${p.solutionCode}" has no code files`);
    }
  }
  for (const group of ['questions', 'followUps'] as const) {
    for (const q of data[group] ?? []) {
      for (const a of q.askedAt ?? [])
        if (!a.source) errors.push(`${f}: askedAt ${a.company} has no source`);
      const b = JSON.stringify(q).match(banned);
      if (b) errors.push(`${f}: contains level/schedule framing "${b[0]}"`);
    }
  }
}

for (const id of snippetIds.keys())
  if (!used.code.has(id)) warn.push(`Orphan snippet: ${id} (${snippetIds.get(id)!.join(', ')})`);
for (const id of diagramIds) if (!used.diagram.has(id)) warn.push(`Orphan diagram: ${id}`);
for (const id of walkIds) if (!used.walk.has(id)) warn.push(`Orphan walkthrough: ${id}`);
for (const f of diagramFiles.filter((f) => f.endsWith('.mmd'))) {
  if (!diagramFiles.includes(f.replace(/\.mmd$/, '.svg')))
    errors.push(`${f}: not rendered — run npm run diagrams`);
}

// Every track needs a pre-rendered social card (npm run og), or its pages point og:image at a 404.
for (const f of await list('src/content/tracks/*.yaml')) {
  const id = stem(f);
  if (!(await list(`public/og/${id}.png`)).length) errors.push(`Track "${id}" has no public/og/${id}.png — run npm run og`);
}

for (const w of warn) console.warn(`warn  ${w}`);
for (const e of errors) console.error(`error ${e}`);
console.log(
  `\n${mdxFiles.length} topics, ${snippetIds.size} snippets, ${diagramIds.size} diagrams, ${walkIds.size} walkthroughs, ${termIds.size} glossary terms.`,
);
console.log(`${errors.length} errors, ${warn.length} warnings.`);
process.exit(errors.length ? 1 : 0);

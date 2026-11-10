/**
 * Writes dist/sw.js: the offline service worker with a list of every built file and its content
 * hash. Runs after `astro build` and Pagefind, so the search index is cached too.
 * The version is a hash of that list, so any content change produces a new worker.
 */
import { createHash } from 'node:crypto';
import { readFile, readdir, writeFile } from 'node:fs/promises';
import { join, relative, sep } from 'node:path';

const root = new URL('../../', import.meta.url).pathname;
const dist = join(root, 'dist');

// Social cards are only fetched by link previews, never by a reader.
const skip = (path: string) =>
  path === 'sw.js' || path.startsWith('og/') || path.endsWith('.DS_Store');

async function walk(dir: string): Promise<string[]> {
  const out: string[] = [];
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) out.push(...(await walk(full)));
    else out.push(full);
  }
  return out;
}

const hash = (data: string | Buffer, length: number) =>
  createHash('sha256').update(data).digest('hex').slice(0, length);

const files: Record<string, string> = {};
let bytes = 0;
for (const full of (await walk(dist)).sort()) {
  const path = relative(dist, full).split(sep).join('/');
  if (skip(path)) continue;
  const data = await readFile(full);
  files[path] = hash(data, 12);
  bytes += data.length;
}

// A fingerprint of each topic's source. Built pages also change with the layout, the footer or a
// new script name, so comparing them can't tell whether a topic's own content changed.
const sources = join(root, 'src/content/topics');
// Lets scripts/audit/offline-update.ts publish content changes without editing a source file
// (comma-separated topic or companies/<slug> ids).
const testChanged = new Set((process.env.EFG_TEST_CHANGED_TOPIC ?? '').split(',').filter(Boolean));
const topics: Record<string, string> = {};
for (const track of (await readdir(sources, { withFileTypes: true })).filter((d) =>
  d.isDirectory(),
)) {
  for (const slug of (await readdir(join(sources, track.name), { withFileTypes: true })).filter(
    (d) => d.isDirectory(),
  )) {
    const id = `${track.name}/${slug.name}`;
    if (!(`${id}/index.html` in files)) continue; // a draft isn't published
    const digest = createHash('sha256');
    for (const full of (await walk(join(sources, id))).sort())
      digest.update(relative(sources, full)).update(await readFile(full));
    if (testChanged.has(id)) digest.update('test');
    topics[id] = digest.digest('hex').slice(0, 12);
  }
}

// Company question lists are tracked the same way, as `companies/<slug>`: the YAML file plus the
// solution files it names from company-questions/code/. Solutions reused from topics are covered
// by those topics' own fingerprints.
const companySources = join(root, 'src/content/company-questions');
const companyCode = await readdir(join(companySources, 'code')).catch(() => [] as string[]);
for (const file of (await readdir(companySources)).filter((f) => f.endsWith('.yaml')).sort()) {
  const id = `companies/${file.replace(/\.yaml$/, '')}`;
  if (!(`${id}/index.html` in files)) continue;
  const yaml = await readFile(join(companySources, file));
  const digest = createHash('sha256').update(file).update(yaml);
  const used = new Set([...yaml.toString().matchAll(/^\s+code: (\S+)$/gm)].map((m) => m[1]));
  for (const f of companyCode.filter((f) => used.has(f.replace(/\.[^.]+$/, ''))).sort())
    digest.update(f).update(await readFile(join(companySources, 'code', f)));
  if (testChanged.has(id)) digest.update('test');
  topics[id] = digest.digest('hex').slice(0, 12);
}

const list = JSON.stringify(files);
const version = hash(list, 10);
const template = await readFile(join(root, 'scripts/pwa/sw.template.js'), 'utf8');
const sw = template
  .replace(/^\/\* global .*\*\/\n/, '')
  .replace('__FILES__', () => list)
  .replace('__TOPICS__', () => JSON.stringify(topics))
  .replace('__VERSION__', () => JSON.stringify(version))
  .replace('__BYTES__', () => String(bytes));
await writeFile(join(dist, 'sw.js'), sw);

const count = Object.keys(files).length;
console.log(
  `sw.js: ${count} files, ${Object.keys(topics).length} topics, ${(bytes / 1e6).toFixed(1)} MB, version ${version}`,
);

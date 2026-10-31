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

const list = JSON.stringify(files);
const version = hash(list, 10);
const template = await readFile(join(root, 'scripts/pwa/sw.template.js'), 'utf8');
const sw = template
  .replace(/^\/\* global .*\*\/\n/, '')
  .replace('__FILES__', () => list)
  .replace('__VERSION__', () => JSON.stringify(version))
  .replace('__BYTES__', () => String(bytes));
await writeFile(join(dist, 'sw.js'), sw);

const count = Object.keys(files).length;
console.log(`sw.js: ${count} files, ${(bytes / 1e6).toFixed(1)} MB, version ${version}`);

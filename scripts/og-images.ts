/**
 * Renders 1200×630 Open Graph cards — one per track plus a site default — into public/og/.
 *   npm run og        (run after adding a track or renaming one; the PNGs are committed)
 */
import { chromium } from 'playwright';
import { mkdir, readFile, readdir } from 'node:fs/promises';
import { join } from 'node:path';
import { parse } from 'yaml';
import { site } from '../src/site.config';

const root = new URL('../', import.meta.url).pathname;
// Inlined as data URLs: a page created with setContent() cannot load file:// fonts.
const font = async (pkg: string, file: string) =>
  `data:font/woff2;base64,${(await readFile(join(root, 'node_modules/@fontsource-variable', pkg, 'files', file))).toString('base64')}`;
const serif = await font('newsreader', 'newsreader-latin-opsz-normal.woff2');
const sans = await font('inter-tight', 'inter-tight-latin-wght-normal.woff2');

interface Track { id: string; title: string; blurb: string; order: number }

async function tracks(): Promise<Track[]> {
  const dir = join(root, 'src/content/tracks');
  const out: Track[] = [];
  for (const f of await readdir(dir)) {
    if (!f.endsWith('.yaml')) continue;
    const t = parse(await readFile(join(dir, f), 'utf8'));
    out.push({ id: f.replace(/\.yaml$/, ''), title: t.title, blurb: t.blurb, order: t.order });
  }
  return out.sort((a, b) => a.order - b.order);
}

async function topicCount(trackId: string): Promise<number> {
  try {
    return (await readdir(join(root, 'src/content/topics', trackId))).length;
  } catch {
    return 0;
  }
}

const escape = (s: string) => s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);

function card(eyebrow: string, title: string, blurb: string, footer: string): string {
  return `<!doctype html><html><head><meta charset="utf-8"><style>
  @font-face { font-family: 'Newsreader'; src: url('${serif}') format('woff2'); font-weight: 200 800; }
  @font-face { font-family: 'Inter Tight'; src: url('${sans}') format('woff2'); font-weight: 100 900; }
  * { margin: 0; box-sizing: border-box; }
  body { width: 1200px; height: 630px; background: #f8f6f1; color: #1c1b19; font-family: 'Inter Tight', sans-serif;
         display: flex; flex-direction: column; padding: 72px 84px; border-left: 18px solid #a2401c; }
  .eyebrow { font-size: 26px; letter-spacing: 0.08em; text-transform: uppercase; color: #8a3415; font-weight: 600; }
  h1 { font-family: 'Newsreader', serif; font-size: 92px; line-height: 1.02; font-weight: 500; margin-top: 28px; letter-spacing: -0.01em; }
  p { font-size: 32px; line-height: 1.35; color: #4a4741; margin-top: 28px; max-width: 980px; }
  footer { margin-top: auto; display: flex; align-items: center; gap: 18px; font-size: 26px; color: #6b6861; }
  footer b { color: #1c1b19; font-weight: 600; }
  </style></head><body>
  <div class="eyebrow">${escape(eyebrow)}</div>
  <h1>${escape(title)}</h1>
  <p>${escape(blurb)}</p>
  <footer><svg width="44" height="44" viewBox="0 0 28 28"><rect x="1.5" y="1.5" width="25" height="25" rx="5" fill="#f8f6f1" stroke="#1c1b19" stroke-width="1.5"/><path d="M8 19V9h8M8 14h6" fill="none" stroke="#a2401c" stroke-width="2.2" stroke-linecap="round"/><circle cx="19.5" cy="18.5" r="2" fill="#a2401c"/></svg>
  <b>${escape(site.name)}</b><span>· ${escape(footer)}</span></footer>
  </body></html>`;
}

const outDir = join(root, 'public/og');
await mkdir(outDir, { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });

const all = await tracks();
const jobs: [string, string][] = [
  ['default', card('Java · JavaScript · Python', site.name, site.tagline, `${all.length} tracks`)],
];
for (const t of all) {
  const n = await topicCount(t.id);
  jobs.push([t.id, card(site.name, t.title, t.blurb, `${n} topics`)]);
}
for (const [name, html] of jobs) {
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: join(outDir, `${name}.png`) });
  console.log(`✓ og/${name}.png`);
}
await browser.close();

/**
 * One-off migration: legacy/sde2.html → drafts/<section>/<NN-slug>/ (git-ignored).
 *
 * Each legacy panel becomes a draft folder with:
 *   meta.json      title, subtitle, legacy section/panel ids
 *   index.md       prose converted to Markdown, with component placeholders
 *   qa.yaml        scenarios → questions, follow-up chains → followUps, practice blocks → practice
 *   code/*.java…   code blocks as plain text (syntax spans stripped, entities decoded)
 *   diagrams/*.svg SVGs with hard-coded colours rewritten to --d-* theme tokens
 *
 * Drafts are raw material for the editorial pass, not publishable content.
 *   npx tsx scripts/migrate/extract.ts
 */
import { load, type Cheerio, type CheerioAPI } from 'cheerio';
import type { AnyNode, Element } from 'domhandler';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import TurndownService from 'turndown';
// @ts-expect-error — the plugin ships no types
import { gfm } from 'turndown-plugin-gfm';
import { stringify } from 'yaml';

const root = new URL('../../', import.meta.url).pathname;
const out = join(root, 'drafts');

const COLOR_MAP: Record<string, string> = {
  '#1a56db': 'var(--d-info)',
  '#1e40af': 'var(--d-info)',
  '#1e3a8a': 'var(--d-info)',
  '#60a5fa': 'var(--d-info)',
  '#93c5fd': 'var(--d-info-soft)',
  '#dbeafe': 'var(--d-info-soft)',
  '#bfdbfe': 'var(--d-info-soft)',
  '#16a34a': 'var(--d-ok)',
  '#166534': 'var(--d-ok)',
  '#dcfce7': 'var(--d-ok-soft)',
  '#bbf7d0': 'var(--d-ok-soft)',
  '#7c3aed': 'var(--d-plum)',
  '#ede9fe': 'var(--d-plum-soft)',
  '#d97706': 'var(--d-warn)',
  '#92400e': 'var(--d-warn)',
  '#fef3c7': 'var(--d-warn-soft)',
  '#fde68a': 'var(--d-warn-soft)',
  '#dc2626': 'var(--d-bad)',
  '#fee2e2': 'var(--d-bad-soft)',
  '#374151': 'var(--d-text)',
  '#0b0e14': 'var(--d-text)',
  '#6b7280': 'var(--d-muted)',
  '#9ca3af': 'var(--d-muted)',
  '#e5e7eb': 'var(--d-line)',
  '#faf9f6': 'var(--d-fill)',
  white: 'var(--d-bg)',
  '#fff': 'var(--d-bg)',
  '#ffffff': 'var(--d-bg)',
};

function themeSvg(svg: string, id: string): string {
  let s = svg
    .replace(
      /(fill|stroke)="rgba\(255,\s*255,\s*255,\s*\.?(\d*)\)"/g,
      (_m, attr, a) => `${attr}="var(--d-bg)" ${attr}-opacity=".${a || '8'}"`,
    )
    .replace(/(fill|stroke|stop-color)="([^"]+)"/g, (m, attr, v: string) => {
      const mapped = COLOR_MAP[v.toLowerCase()];
      return mapped ? `${attr}="${mapped}"` : m;
    })
    .replace(/font-family="IBM Plex Mono"/g, 'font-family="var(--font-mono)"')
    .replace(/font-family="IBM Plex Sans"/g, 'font-family="var(--font-sans)"');
  // Marker/gradient ids must be unique once many SVGs are inlined on one page.
  const ids = [...s.matchAll(/id="([^"]+)"/g)].map((m) => m[1]!);
  for (const old of ids)
    s = s.replaceAll(`"${old}"`, `"${id}-${old}"`).replaceAll(`#${old})`, `#${id}-${old})`);
  return s;
}

const slug = (t: string) =>
  t
    .toLowerCase()
    .replace(/&/g, ' and ')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 60);

const stripEmoji = (t: string) =>
  t
    .replace(/[\p{Extended_Pictographic}\u{FE0F}\u{200D}]/gu, '')
    .replace(/\s+/g, ' ')
    .trim();

const LANG_EXT: [RegExp, string][] = [
  [/java\b/i, 'java'],
  [/javascript|node|\bjs\b/i, 'js'],
  [/typescript|\bts\b/i, 'ts'],
  [/python/i, 'py'],
  [/sql/i, 'sql'],
  [/shell|bash|git\b|terminal|cli/i, 'sh'],
  [/yaml|yml|kubernetes|k8s|compose|actions/i, 'yaml'],
  [/json/i, 'json'],
  [/docker/i, 'dockerfile'],
  [/properties|application\./i, 'properties'],
  [/xml|pom/i, 'xml'],
  [/http|rest|curl/i, 'http'],
];

function guessExt(label: string, code: string): string {
  for (const [re, ext] of LANG_EXT) if (re.test(label)) return ext;
  if (/^\s*(SELECT|CREATE|INSERT|UPDATE|WITH)\b/im.test(code)) return 'sql';
  if (/\b(public|private|class|interface|void|@Override|new [A-Z])\b/.test(code)) return 'java';
  return 'txt';
}

function makeTurndown($: CheerioAPI) {
  const td = new TurndownService({
    headingStyle: 'atx',
    codeBlockStyle: 'fenced',
    bulletListMarker: '-',
  });
  td.use(gfm);
  td.remove(['script', 'style']);
  td.addRule('legacyLabels', {
    filter: (node) =>
      /\b(think-label|approach-label|h-label|cb-label|s-label|pv-label|df-header)\b/.test(
        node.getAttribute?.('class') ?? '',
      ),
    replacement: () => '',
  });
  void $;
  return td;
}

interface Draft {
  section: string;
  panelId: string;
  title: string;
  sub: string;
  dir: string;
  md: string[];
  qa: { questions: object[]; followUps: object[]; practice: object[] };
  snippets: { id: string; ext: string; code: string; title: string }[];
  diagrams: { id: string; svg: string; caption: string }[];
}

async function main() {
  const html = await readFile(join(root, 'legacy/sde2.html'), 'utf8');
  const $ = load(html);
  const td = makeTurndown($);
  const toMd = (el: Cheerio<AnyNode>) => td.turndown($.html(el) ?? '').trim();
  await rm(out, { recursive: true, force: true });

  const sections = $('[id]').filter(
    (_, el) =>
      $(el).find('.wcontent-panel').length > 0 &&
      !$(el)
        .parents()
        .filter((_, p) => $(p).find('.wcontent-panel').length > 0 && !!$(p).attr('id')).length,
  );
  const inventory: object[] = [];
  let snippetCount = 0;

  for (const sectionEl of sections.toArray()) {
    const section = $(sectionEl).attr('id')!;
    const panels = $(sectionEl).find('.wcontent-panel').toArray();
    for (const [n, panelEl] of panels.entries()) {
      const panel = $(panelEl);
      const title = stripEmoji(
        panel.find('.wcontent-head h3').first().text() ||
          $(sectionEl).find('h3').first().text() ||
          section,
      );
      const sub = stripEmoji(panel.find('.wcontent-head .wc-sub').first().text());
      const s = slug(title);
      const d: Draft = {
        section,
        panelId: panel.attr('id') ?? `${section}-p${n}`,
        title,
        sub,
        dir: join(out, section, `${String(n + 1).padStart(2, '0')}-${s}`),
        md: [],
        qa: { questions: [], followUps: [], practice: [] },
        snippets: [],
        diagrams: [],
      };
      const snippet = (codeEl: Cheerio<Element>, fallbackTitle: string) => {
        const label = stripEmoji(codeEl.find('.code-lang').first().text());
        const raw = codeEl.find('.code-inner').first().text();
        const code =
          raw
            .replace(/\u00a0/g, ' ')
            .replace(/[ \t]+$/gm, '')
            .replace(/^\n+|\n+$/g, '') + '\n';
        const id = `${s}-${++snippetCount}`;
        d.snippets.push({ id, ext: guessExt(label, code), code, title: label || fallbackTitle });
        return { id, title: label || fallbackTitle };
      };

      for (const child of panel.children().toArray()) {
        const c = $(child);
        const cls = c.attr('class') ?? '';
        if (cls.includes('wcontent-head')) continue;

        if (cls.includes('scenario-box') || cls.includes('prod-scenario')) {
          d.qa.questions.push({
            q: stripEmoji(c.find('.s-q').text()),
            think: toMd(c.find('.s-think')),
            answer: toMd(c.find('.s-ans')),
          });
          continue;
        }
        if (cls.startsWith('s-')) continue; // stray scenario parts handled below
        if (cls.includes('depth-followup')) {
          for (const item of c.find('.df-item').toArray()) {
            const it = $(item);
            d.qa.followUps.push({
              q: stripEmoji(it.find('.df-q').text())
                .replace(/^Q:\s*/, '')
                .replace(/^['"“]|['"”]$/g, ''),
              intuition: toMd(it.find('.df-intuition')),
              approach: toMd(it.find('.df-approach')),
            });
          }
          continue;
        }
        if (cls.includes('practice-v6')) {
          const codeEl = c.find('.code').first();
          const sol = codeEl.length ? snippet(codeEl, 'Solution') : undefined;
          const solText = c.clone();
          solText.find('.pv-label,.pv-task,.pv-hint,.pv-solution-title,.code').remove();
          d.qa.practice.push({
            task: toMd(c.find('.pv-task')),
            hint: toMd(c.find('.pv-hint')),
            solution: toMd(solText) || '(see code)',
            ...(sol ? { solutionCode: sol.id } : {}),
          });
          continue;
        }
        if (cls.includes('code')) {
          const sn = snippet(c, 'Code');
          d.md.push(`<CodeTabs id="${sn.id}" title="${sn.title.replace(/"/g, "'")}" />`);
          continue;
        }
        if (cls.includes('cb')) {
          const label = stripEmoji(c.find('.cb-label').first().text());
          if (label) d.md.push(`### ${label}`);
          for (const part of c.children().toArray()) {
            const p = $(part);
            const pc = p.attr('class') ?? '';
            if (pc.includes('cb-label')) continue;
            if (pc.includes('diagram-wrap')) {
              const svg = $.html(p.find('svg').first());
              const caption = stripEmoji(p.find('.diagram-caption').text()) || label;
              const id = `${s}-d${d.diagrams.length + 1}`;
              d.diagrams.push({ id, svg: themeSvg(svg, id), caption });
              d.md.push(`<Diagram id="${id}" caption="${caption.replace(/"/g, "'")}" />`);
            } else if (pc.includes('walkthrough')) {
              const tabs = p
                .find('.wt-tab')
                .toArray()
                .map((t) => stripEmoji($(t).text()));
              p.find('.wt-panel')
                .toArray()
                .forEach((wp, i) => {
                  const codeEl = $(wp).find('.code').first();
                  if (codeEl.length) {
                    const sn = snippet(codeEl, tabs[i] ?? 'Step');
                    d.md.push(
                      `<CodeTabs id="${sn.id}" title="${(tabs[i] ?? sn.title).replace(/"/g, "'")}" />`,
                    );
                  } else d.md.push(toMd($(wp)));
                });
            } else if (pc.includes('code')) {
              const sn = snippet(p as Cheerio<Element>, label);
              d.md.push(`<CodeTabs id="${sn.id}" title="${sn.title.replace(/"/g, "'")}" />`);
            } else if (pc.includes('pitfalls')) {
              d.md.push(`<Pitfalls>\n\n${toMd(p)}\n\n</Pitfalls>`);
            } else if (pc.includes('intuition') || pc.includes('analogy')) {
              d.md.push(
                `<Callout type="${pc.includes('analogy') ? 'analogy' : 'note'}">\n\n${toMd(p)}\n\n</Callout>`,
              );
            } else {
              d.md.push(toMd(p));
            }
          }
          continue;
        }
        if (cls.includes('diagram-wrap')) {
          const id = `${s}-d${d.diagrams.length + 1}`;
          d.diagrams.push({
            id,
            svg: themeSvg($.html(c.find('svg').first()), id),
            caption: stripEmoji(c.find('.diagram-caption').text()),
          });
          d.md.push(`<Diagram id="${id}" caption="" />`);
          continue;
        }
        d.md.push(toMd(c));
      }
      // Stray top-level scenario parts (one panel uses them without a wrapper).
      const strayQ = panel.children('.s-q');
      if (strayQ.length) {
        d.qa.questions.push({
          q: stripEmoji(strayQ.text()),
          think: toMd(panel.children('.s-think')),
          answer: toMd(panel.children('.s-ans')),
        });
      }

      await mkdir(join(d.dir, 'code'), { recursive: true });
      await mkdir(join(d.dir, 'diagrams'), { recursive: true });
      await writeFile(
        join(d.dir, 'meta.json'),
        JSON.stringify({ title, sub, section, panelId: d.panelId }, null, 2),
      );
      await writeFile(
        join(d.dir, 'index.md'),
        `# ${title}\n\n> ${sub}\n\n${d.md.filter(Boolean).join('\n\n')}\n`,
      );
      await writeFile(join(d.dir, 'qa.yaml'), stringify(d.qa, { lineWidth: 0 }));
      for (const sn of d.snippets)
        await writeFile(join(d.dir, 'code', `${sn.id}.${sn.ext}`), sn.code);
      for (const dg of d.diagrams) await writeFile(join(d.dir, 'diagrams', `${dg.id}.svg`), dg.svg);
      inventory.push({
        section,
        panelId: d.panelId,
        title,
        dir: d.dir.replace(root, ''),
        questions: d.qa.questions.length,
        followUps: d.qa.followUps.length,
        practice: d.qa.practice.length,
        snippets: d.snippets.length,
        diagrams: d.diagrams.length,
      });
    }
  }
  await writeFile(join(out, 'inventory.json'), JSON.stringify(inventory, null, 2));
  console.log(`${inventory.length} panels → drafts/ (${snippetCount} snippets)`);
}

await main();

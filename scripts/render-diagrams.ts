/**
 * Renders every `diagrams/*.mmd` (Mermaid source) under src/content/topics to a sibling `.svg`.
 *
 * Mermaid's own colours are stripped and replaced with a stylesheet that uses the site's
 * `--d-*` CSS variables, so diagrams follow the light/dark theme with no runtime JS.
 * Output is committed; CI and normal builds never need a browser.
 *
 *   npm run diagrams            render changed diagrams
 *   npm run diagrams -- --force re-render everything
 *   npm run diagrams -- --check exit 1 if any .svg is missing or stale (used in CI)
 */
import { createHash } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { glob } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { basename, dirname, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const RENDERER_VERSION = '7';
const root = new URL('../', import.meta.url).pathname;
const args = new Set(process.argv.slice(2));

const themeCss = (id: string) => `
#${id}{font-family:var(--font-sans);font-size:16px;line-height:1.5;color:var(--d-text);}
#${id} foreignObject{overflow:visible;}
#${id} .label,#${id} .nodeLabel,#${id} .edgeLabel,#${id} text,#${id} span,#${id} p{color:var(--d-text);fill:var(--d-text);}
#${id} .node rect,#${id} .node circle,#${id} .node ellipse,#${id} .node polygon,#${id} .node path,
#${id} rect.actor,#${id} .classGroup rect,#${id} .stateGroup rect,#${id} .er.entityBox,#${id} .note rect{fill:var(--d-fill);stroke:var(--d-stroke);stroke-width:1.4px;}
#${id} .cluster rect{fill:transparent;stroke:var(--d-line);stroke-dasharray:4 3;rx:6px;}
#${id} .cluster-label span,#${id} .cluster-label p,#${id} .cluster text{color:var(--d-muted);fill:var(--d-muted);}
#${id} .flowchart-link,#${id} .edgePath .path,#${id} path.relation,#${id} .messageLine0,#${id} .messageLine1,#${id} .transition,#${id} line{stroke:var(--d-stroke);stroke-width:1.4px;fill:none;}
#${id} marker path,#${id} .arrowheadPath,#${id} .marker{fill:var(--d-stroke);stroke:var(--d-stroke);}
#${id} .edgeLabel,#${id} .edgeLabel rect,#${id} .labelBkg{background:var(--d-bg);fill:var(--d-bg);}
#${id} .node.accent rect,#${id} .node.accent polygon,#${id} .node.accent path,#${id} .node.accent circle{fill:var(--d-accent-soft);stroke:var(--d-accent);}
#${id} .node.ok rect,#${id} .node.ok polygon,#${id} .node.ok path{fill:var(--d-ok-soft);stroke:var(--d-ok);}
#${id} .node.info rect,#${id} .node.info polygon,#${id} .node.info path{fill:var(--d-info-soft);stroke:var(--d-info);}
#${id} .node.warn rect,#${id} .node.warn polygon,#${id} .node.warn path{fill:var(--d-warn-soft);stroke:var(--d-warn);}
#${id} .node.bad rect,#${id} .node.bad polygon,#${id} .node.bad path{fill:var(--d-bad-soft);stroke:var(--d-bad);}
#${id} .node.muted rect,#${id} .node.muted polygon{fill:transparent;stroke:var(--d-line);stroke-dasharray:3 3;}
#${id} rect.note{fill:var(--d-warn-soft);stroke:var(--d-warn);stroke-width:1px;}
#${id} .messageText,#${id} text.actor,#${id} text.actor>tspan,#${id} .noteText,#${id} .noteText>tspan,#${id} .sequenceNumber{fill:var(--d-text);stroke:none;}
#${id} .actor-line{stroke:var(--d-line);}
#${id} .activation0,#${id} .activation1,#${id} .activation2{fill:var(--d-accent-soft);stroke:var(--d-accent);}
#${id} .loopLine{stroke:var(--d-line);}
#${id} .labelBox{fill:var(--d-fill);stroke:var(--d-line);}
#${id} .loopText,#${id} .loopText>tspan,#${id} .labelText,#${id} .labelText>tspan{fill:var(--d-muted);}
`;

function postProcess(svg: string, id: string, hash: string): string {
  return (
    `<!-- generated from ${id}.mmd by scripts/render-diagrams.ts; hash:${hash} -->\n` +
    svg
      .replace(/<\/style>/, `${themeCss(`mmd-${id}`).replace(/\n/g, '')}</style>`)
      .replace(/ style="max-width: [^"]*"/, '')
      .replace(/background-color:\s*[^;"]+;?/g, '')
  );
}

const hashOf = (src: string) =>
  createHash('sha256')
    .update(RENDERER_VERSION + src)
    .digest('hex')
    .slice(0, 16);

async function main() {
  const files: string[] = [];
  for await (const f of glob('src/content/topics/**/diagrams/*.mmd', { cwd: root }))
    files.push(join(root, f));

  const pending: { file: string; out: string; id: string; src: string; hash: string }[] = [];
  for (const file of files) {
    const src = await readFile(file, 'utf8');
    const hash = hashOf(src);
    const out = file.replace(/\.mmd$/, '.svg');
    const existing = await readFile(out, 'utf8').catch(() => '');
    if (args.has('--force') || !existing.includes(`hash:${hash}`)) {
      pending.push({ file, out, id: basename(file, '.mmd'), src, hash });
    }
  }

  if (args.has('--check')) {
    if (pending.length) {
      console.error(
        `Stale or missing diagram SVGs:\n${pending.map((p) => `  ${p.file}`).join('\n')}\nRun: npm run diagrams`,
      );
      process.exit(1);
    }
    console.log(`All ${files.length} diagrams up to date.`);
    return;
  }
  if (!pending.length) {
    console.log(`All ${files.length} diagrams up to date.`);
    return;
  }

  const { createMermaidRenderer } = await import('mermaid-isomorphic');
  const require = createRequire(import.meta.url);
  const fontCss = pathToFileURL(require.resolve('@fontsource-variable/inter-tight/index.css'));
  const render = createMermaidRenderer();
  const results = await render(
    pending.map((p) => p.src),
    {
      css: fontCss,
      prefix: 'mmd',
      mermaidConfig: {
        theme: 'base',
        fontFamily: '"Inter Tight Variable", sans-serif',
        flowchart: { curve: 'basis', padding: 12, nodeSpacing: 36, rankSpacing: 44 },
        sequence: { mirrorActors: false },
      },
    },
  );

  let failed = 0;
  for (const [i, result] of results.entries()) {
    const p = pending[i]!;
    if (result.status === 'rejected') {
      failed++;
      console.error(`✗ ${p.file}\n  ${String(result.reason)}`);
      continue;
    }
    // Rename the generated id everywhere (root id, marker ids and their url(#…) references).
    const oldId = result.value.id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const svg = result.value.svg.replace(
      new RegExp(`(?<![\\w-])${oldId}(?!\\d)`, 'g'),
      `mmd-${p.id}`,
    );
    await writeFile(p.out, postProcess(svg, p.id, p.hash));
    console.log(`✓ ${join(basename(dirname(dirname(p.file))), 'diagrams', `${p.id}.svg`)}`);
  }
  process.exit(failed ? 1 : 0);
}

await main();

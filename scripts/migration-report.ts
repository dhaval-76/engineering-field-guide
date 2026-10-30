/**
 * Migration completeness: every legacy panel (scripts/migrate/mapping.json) must have
 * its target topic(s) published, and the target's Q&A should cover the legacy Q&A volume.
 *   npm run report:migration           report
 *   npm run report:migration -- --strict  exit 1 on any missing topic
 */
import { access, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { parse } from 'yaml';

interface Row {
  panelId: string;
  title: string;
  targets: string[];
  questions: number;
  followUps: number;
  practice: number;
}

const root = new URL('../', import.meta.url).pathname;
const rows: Row[] = JSON.parse(await readFile(join(root, 'scripts/migrate/mapping.json'), 'utf8'));
const exists = (p: string) =>
  access(p).then(
    () => true,
    () => false,
  );

const byTarget = new Map<string, { legacy: Row[] }>();
for (const r of rows)
  for (const t of r.targets) byTarget.set(t, { legacy: [...(byTarget.get(t)?.legacy ?? []), r] });

let missing = 0;
let thin = 0;
const lines: string[] = [];
for (const [target, { legacy }] of [...byTarget.entries()].sort()) {
  const dir = join(root, 'src/content/topics', target);
  if (!(await exists(join(dir, 'index.mdx')))) {
    missing++;
    lines.push(`MISSING  ${target}  ← ${legacy.map((l) => l.panelId).join(', ')}`);
    continue;
  }
  const qa = (await exists(join(dir, 'qa.yaml')))
    ? (parse(await readFile(join(dir, 'qa.yaml'), 'utf8')) ?? {})
    : {};
  // A legacy panel split across several targets contributes its Q&A once, to its first target.
  const owned = legacy.filter((l) => l.targets[0] === target);
  const want = owned.reduce((n, l) => n + l.questions + l.followUps, 0);
  const have = (qa.questions?.length ?? 0) + (qa.followUps?.length ?? 0);
  const flag = have < Math.min(want, 4) ? 'THIN   ' : 'ok     ';
  if (flag.startsWith('THIN')) thin++;
  lines.push(
    `${flag}  ${target}  Q&A ${have}/${want} legacy  ← ${legacy.map((l) => l.panelId).join(', ')}`,
  );
}
console.log(lines.join('\n'));
console.log(
  `\n${byTarget.size} target topics from ${rows.length} legacy panels: ${byTarget.size - missing} published, ${missing} missing, ${thin} thin.`,
);
if (process.argv.includes('--strict') && missing) process.exit(1);

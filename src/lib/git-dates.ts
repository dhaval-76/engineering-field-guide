import { execFileSync } from 'node:child_process';

let cache: Map<string, string> | undefined;

/**
 * When each topic was first added, from git history (topic id → YYYY-MM-DD). Moves and renames are
 * followed, so a topic keeps its original date. Empty when history is unavailable or shallow (CI
 * must check out with fetch-depth: 0); callers then fall back to `lastReviewed`.
 */
export function topicAddedDates(): Map<string, string> {
  if (cache) return cache;
  cache = new Map();
  try {
    const git = (...args: string[]) =>
      execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 << 20 });
    if (git('rev-parse', '--is-shallow-repository').trim() === 'true') return cache;
    const log = git(
      'log',
      '--reverse',
      '-M',
      '--name-status',
      '--format=@%cs',
      '--',
      'src/content/topics',
    );
    const id = (path: string) =>
      path.match(/^src\/content\/topics\/([^/]+\/[^/]+)\/index\.mdx$/)?.[1];
    const added = new Map<string, string>();
    let date = '';
    for (const line of log.split('\n')) {
      if (line.startsWith('@')) date = line.slice(1);
      const [status, a, b] = line.split('\t');
      if (status === 'A' && a && id(a)) added.set(id(a)!, added.get(id(a)!) ?? date);
      else if (status?.startsWith('R') && a && b && id(b))
        added.set(id(b)!, (a && id(a) && added.get(id(a)!)) || date);
    }
    cache = added;
  } catch {
    /* no git (e.g. building from a tarball): no added dates */
  }
  return cache;
}

/**
 * Scaffold a topic folder with the standard template.
 *   npm run new:topic -- <track>/<slug> ["Title"]
 */
import { access, mkdir, readdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const [path, titleArg] = process.argv.slice(2);
if (!path || !/^[a-z0-9-]+\/[a-z0-9-]+$/.test(path)) {
  console.error('Usage: npm run new:topic -- <track>/<slug> ["Title"]   (lowercase, hyphenated)');
  process.exit(1);
}
const [track, slug] = path.split('/') as [string, string];
const root = new URL('../', import.meta.url).pathname;
const tracks = (await readdir(join(root, 'src/content/tracks'))).map((f) =>
  f.replace(/\.yaml$/, ''),
);
if (!tracks.includes(track)) {
  console.error(`Unknown track "${track}". Known: ${tracks.join(', ')}`);
  process.exit(1);
}
const dir = join(root, 'src/content/topics', track, slug);
if (
  await access(dir).then(
    () => true,
    () => false,
  )
) {
  console.error(`Already exists: ${dir}`);
  process.exit(1);
}
const title = titleArg ?? slug.replace(/-/g, ' ').replace(/^\w/, (c) => c.toUpperCase());
const today = new Date().toISOString().slice(0, 10);

await mkdir(join(dir, 'code'), { recursive: true });
await mkdir(join(dir, 'diagrams'), { recursive: true });
await mkdir(join(dir, 'walkthroughs'), { recursive: true });

await writeFile(
  join(dir, 'index.mdx'),
  `---
title: ${title}
summary: One sentence that tells a reader what this is and why they'd care.
track: ${track}
order: 100
tags: []
aliases: []
quickRef:
  points:
    - The single most important fact.
  useWhen: When to reach for it.
  complexity: Time/space or operational cost.
draft: true
lastReviewed: ${today}
---

## Why it matters

<Callout type="analogy">
A plain-language picture of the idea.
</Callout>

## The core idea

<!-- <Diagram id="${slug}-overview" caption="..." /> -->

## How it works, step by step

<!-- <Walkthrough id="${slug}-steps" /> -->

<Steps>

1. First step.

</Steps>

## Implementation

<CodeTabs id="${slug}-basic" title="${title}" />

## Trade-offs and variations

| Option | Good for | Weak spot |
|---|---|---|
|  |  |  |

## In production

<Pitfalls>

- **Mistake** — why it hurts and what to do instead.

</Pitfalls>

<KeyTakeaways>

- Takeaway.

</KeyTakeaways>
`,
);
await writeFile(join(dir, 'code', `${slug}-basic.java`), `// Java version (primary)\n`);
await writeFile(join(dir, 'code', `${slug}-basic.js`), `// JavaScript version\n`);
await writeFile(
  join(dir, 'qa.yaml'),
  `questions: []
followUps: []
practice: []
quiz: []
`,
);
console.log(
  `Created ${dir}\nIt is marked draft: true — visible in dev, hidden in production builds until you remove that line.`,
);

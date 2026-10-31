# Engineering Field Guide

Software-engineering concepts explained visually, for quick daily reference and for learning or interview prep. It is a static site built with Astro + MDX + React islands (TypeScript) and deployed to GitHub Pages.

## Run it

```bash
npm install
npm run dev            # http://localhost:4321/engineering-field-guide/
npm run build          # static site + search index in dist/
npm run preview
```

Requires Node 22+.

## How content is organised

Content is stored as data, never as page HTML:

```
src/content/
├─ tracks/<track>.yaml                 # a track: title, blurb, order, icon
├─ glossary/<term>.yaml                # short definitions, shown inline via <Term id="…">
└─ topics/<track>/<slug>/
   ├─ index.mdx                        # frontmatter + prose
   ├─ qa.yaml                          # interview questions, follow-ups, practice, quiz
   ├─ code/<id>.java|.js|.py|…         # one snippet = one file per language
   ├─ diagrams/<id>.svg | <id>.mmd     # hand-drawn SVG or Mermaid source
   └─ walkthroughs/<id>.yaml           # step-by-step visual frames
```

Schemas live in `src/content.config.ts`. A missing field, a wrong type or a reference to a topic that doesn't exist fails the build.

### Add a topic

```bash
npm run new:topic -- distributed-systems/consistent-hashing "Consistent Hashing"
```

This scaffolds the standard template and marks the topic `draft: true`, which keeps it out of production builds until you delete that line. Topics follow the same shape:

1. Quick reference, written in frontmatter (`quickRef`)
2. Why it matters, with an analogy
3. The core idea, with a diagram
4. How it works, as a walkthrough or steps
5. Implementation in code tabs
6. Trade-offs, as tables or charts
7. In production, with pitfalls
8. Key takeaways
9. The interview section, generated from `qa.yaml`

### Components available in MDX (no imports needed)

| Component | Use |
|---|---|
| `<CodeTabs id="x" title="…" />` | Renders every `code/x.*` file as language tabs. Java comes first, then JavaScript, then Python. |
| `<Diagram id="x" caption="…" />` | Inlines `diagrams/x.svg` |
| `<Walkthrough id="x" />` | Step-through player for `walkthroughs/x.yaml` |
| `<Chart type="bar\|line" … />` | Small build-time SVG chart |
| `<Callout type="note\|tip\|warning\|insight\|analogy\|prereq">` | Asides |
| `<Steps>`, `<Pitfalls>`, `<KeyTakeaways>` | Wrap a Markdown list |
| `<Term id="x">text</Term>` | Inline glossary definition |

Snippet, diagram and walkthrough ids are global, so keep them unique (prefix them with the topic, e.g. `lru-…`).

### Adding a language

Drop `code/<id>.<ext>` next to the existing files. To add a new language, add it to `codeLanguages` in `src/site.config.ts`.

### Diagrams

- Hand-drawn SVGs must use the `--d-*` colour variables (see `src/styles/tokens.css`) so they work in both themes.
- Mermaid sources (`.mmd`) are rendered once by `npm run diagrams` (this needs `npx playwright install chromium`), and the generated `.svg` is committed. CI runs `npm run diagrams -- --check` to catch stale renders.

### Company tags

`askedAt` on a topic or question records where a candidate publicly reported being asked it. Every entry needs a `source` URL. These tags drive the `/companies` pages.

## Checks

```bash
npm run lint
npm run check          # astro check + scripts typecheck
npm run check:content  # dangling ids, orphan files, missing sources, leftover level framing
```

The GitHub Actions workflow in `.github/workflows/deploy.yml` runs all of these, builds, and deploys to Pages on every push to `main`. In the repository settings, set **Pages → Source** to **GitHub Actions**. The repository name and base path are set in `src/site.config.ts`.

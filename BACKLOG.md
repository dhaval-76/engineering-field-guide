# Backlog

Agreed work, saved on 2026-11-01.

**Order set by the owner (2026-11-01): 11 → 6 → 7 → 8 → 9 → 10 → 4 → 3 → 1 → 2 → 5.**
Work top to bottom. The numbers in the headings below are labels, not the order.

**Standing rule (item 11 is done, so this applies now):** every later change updates the docs it affects
(`README.md`, `AGENTS.md`, this backlog, `research/README.md`) in the same commit, so they never
drift from the code.

Every content item follows the usual process: research first (demand, interview reports and
"asked at" tags, each with a source URL), then write. One commit per topic group. Push only at
milestones, never after every commit. No AI attribution anywhere.

## Content

### 1. CS Foundations: split Networking and Operating Systems into full pages

Today `cs-foundations/networking` and `cs-foundations/operating-systems` are about 1,000 words each,
much thinner than the rest of the site. Turn them into 9 fully written pages, each with
walkthroughs, interview questions (`qa.yaml`) and a quiz. The current URLs become the first page of
each group, so existing links keep working. Update `related`/`prerequisites` links across the site.

**Networking (5 pages)**

1. **How a request travels:** the layers you actually deal with; what happens when you open a URL
   (keep the `request-journey` walkthrough).
2. **TCP and UDP:**
   - the handshake and how TCP recovers lost packets (retransmission);
   - flow control vs congestion control, and head-of-line blocking;
   - keep-alive, TIME_WAIT and Nagle's algorithm;
   - why games and calls use UDP.

   Walkthroughs: the handshake; a lost packet and its retransmit.
3. **HTTP/1.1, HTTP/2 and HTTP/3:**
   - multiplexing and QUIC;
   - caching headers (Cache-Control, ETag);
   - status codes, cookies and CORS.
4. **DNS and TLS:**
   - DNS resolution step by step, record types and TTLs;
   - the TLS 1.3 handshake, certificate chains and mTLS.

   Walkthroughs: a DNS lookup; the TLS handshake.
5. **Real-time connections:** polling, long polling, Server-Sent Events and WebSockets compared, and
   how load balancers and proxies affect each.

**Operating Systems (4 pages)**

1. **Processes and threads:** fork and exec, scheduling, context switches and their cost, signals.
2. **Memory:**
   - virtual memory, paging, the TLB and page faults;
   - the stack vs the heap, and the OOM killer;
   - CPU caches and false sharing (link to Concurrency).
3. **Files and I/O:**
   - file descriptors, the page cache and fsync;
   - blocking vs non-blocking I/O, epoll and io_uring;
   - zero-copy (`sendfile`, and why Kafka is fast).

   Walkthrough: an epoll event loop.
4. **Containers in OS terms:**
   - namespaces and cgroups;
   - CPU throttling and memory limits;
   - why the JVM needs container-aware settings.

   Open question for the owner: keep this page here or move it to DevOps.

### 2. Security track (new track)

- The OWASP Top 10 for backend engineers, with walkthroughs for SQL injection, XSS, CSRF and SSRF.
- Secrets management (vaults, rotation, never in git or images).
- Encryption at rest and in transit, and key management.
- Signing and verifying webhooks (HMAC, timestamps, replay protection).

### 3. API styles

gRPC, GraphQL and API versioning, compared with REST: when to use which, the trade-offs, and
streaming.

### 4. More AI Engineering

- Evaluating LLM output.
- Prompt caching and cost control.
- Guardrails and prompt-injection defence.
- Vector database internals (HNSW).
- Fine-tuning vs RAG.

### 5. More DevOps

- Blue/green and canary deployments.
- Infrastructure-as-code basics.
- Observability on Kubernetes.

## Site features

All personal data stays in the browser (localStorage keys prefixed `efg:`; see `src/lib/storage.ts`).
Every read and write must stay wrapped in try/catch. Every new page must work offline: the service
worker caches every built file automatically, but check it with the offline crawl.

### 6. Review page (spaced repetition) (done 2026-11-01)

Done: `/review/` with due, learning, remembered and not-tried counts, a track filter, rounds of
10 (missed first), new questions optionally only from completed topics, and due counts by topic.
The topic self-check records every answer and marks questions missed last time. Storage: `efg:quiz`
(see `AGENTS.md`). Quiz history in the data page (9d) can read the same key.

Original scope:

A page that asks quiz questions you got wrong or haven't seen in a while. This means storing quiz
answers per question (see also 9d).

### 7. Keyboard shortcuts: a help panel and a page (done 2026-11-01)

Done: `?` opens a shortcuts panel; `/shortcuts/` lists everything from one shared list
(`src/lib/shortcuts.ts`); footer link and a "Press ? for shortcuts" hint on keyboard devices; a link
from the Offline page. New keys: `t r b m w`, `1`–`5`, `g h` / `g p` / `g r` / `g o`, `[` / `]`,
with a short confirmation for actions that may be off-screen. Also fixed: after closing search,
focus stayed in its hidden input and blocked shortcuts until the next click.

Original scope:

Existing shortcuts, which nothing documents yet:

| Shortcut | What it does |
|---|---|
| `/` or `Ctrl/⌘ K` | open search |
| `↑` / `↓`, `Enter` | move through search results and open one |
| `j` / `k` | next / previous topic |
| `←` / `→` | step a walkthrough, or switch code tabs |
| `Esc` | close search, the menu or the diagram viewer |

Plan:

- Pressing `?` anywhere opens a shortcuts panel; a `/shortcuts/` page lists everything.
- Link to it from the footer and the Offline page, and show a small "Press ? for shortcuts" hint.

Suggested new shortcuts:

- `t` theme; `r` reading mode; `b` bookmark; `m` mark complete; `w` wrap code;
- `1`–`5` switch code language;
- `g h` / `g p` / `g o` go to the home, progress or offline page;
- `[` / `]` previous or next section on a page.

Shortcuts must be ignored while typing in inputs, the same as the existing handler in
`src/scripts/site.ts`.

### 8. History, both meanings

**a. Your reading history**

- Every topic opened, newest first, grouped by day (Today, Yesterday, This week, Earlier).
- Search over the list, and per-item or clear-all removal.
- Today `recent` keeps only 8 entries; raise it to about 200.
- The homepage "recently viewed" still shows the first few.

**b. What's new on the site**

- Topics added or updated recently, from each topic's `lastReviewed` date (and git dates at build
  time if useful).
- With the offline app, also show "what changed in the update you just got": the service worker
  already knows which files changed.

### 9. "Your data" page: view, export, import

What the browser stores today:

- progress, bookmarks and recent history;
- language, theme, reading mode and code wrap preferences;
- the offline copy setting (`offline-optin`, `offline-checked`).

Plan:

- **View:** each item in plain words ("42 topics completed", "Theme: dark") with a raw JSON view.
- **Export:** a versioned `.json` file (`{ app, schemaVersion, exportedAt, data }`), either
  everything or chosen sections.
- **Import:**
  - merge (combine progress and bookmarks, newest wins) or replace;
  - preview the changes first;
  - validate the file, and reject unknown or newer schema versions with a clear message.
- **Delete:** per section or everything, with a confirm step.

Suggestions to build in as well:

- **a. Device-to-device sync with no account:** the phone shows a QR code or a copy-paste code; the
  laptop scans or pastes it and the data merges.
- **b. A backup reminder:** a gentle prompt if progress hasn't been exported in 30 days, because
  clearing browser data wipes it.
- **c. Per-topic private notes:** a note box on each topic, saved locally and included in export.
- **d. Quiz history:** saved quiz answers (needed for item 6) shown and exported here.

### 10. Notes and highlights

Select text on a topic to highlight it or attach a note. Collect them all on one page, and include
them in export and import. This extends 9c.

## Repo housekeeping

### 11. Update all documentation and add AGENTS.md (done 2026-11-01)

Done: `README.md` and `research/README.md` refreshed, `AGENTS.md` added, the walkthrough legend
check moved into `check:content`, and the mobile, offline and update audits moved to
`scripts/audit/` as `npm run audit:mobile`, `audit:offline` and `test:offline-update`.

Original scope:


- **`README.md`:**
  - current features (offline app, walkthrough legends, reading mode, glossary, cheat sheets,
    company tags);
  - every npm script, including `icons` and the service worker step in `build`;
  - the content model and how to add a topic, snippet, diagram, walkthrough or glossary term.
- **`research/README.md`:** refresh the method and dates.
- **`AGENTS.md`** (new), so any new session or agent can pick up work without the history:
  - **Purpose and audience:** a self-paced visual reference and interview-prep site; no levels or
    weeks; Java first, then JavaScript and Python.
  - **Stack and layout:**
    - Astro, MDX, React islands, TypeScript strict;
    - content collections and their Zod schemas in `src/content.config.ts`;
    - where everything lives.
  - **House rules:**
    - no AI attribution in commits, PRs or files;
    - push only at milestones;
    - research before writing content;
    - every "asked at" tag needs a source URL;
    - use the editorial look (no gradients, emoji or template look);
    - mobile first: 16px gutters, no horizontal page scroll;
    - everything must work offline.
  - **Writing style:** plain English, short sentences, an analogy per topic, the standard topic
    template sections, Quick Reference cards.
  - **Walkthrough rules:**
    - colour tones and what each means; every walkthrough needs a `legend:` that labels exactly
      the tones it uses;
    - keep a tone consistent within one walkthrough;
    - check with `.cache/wt-check.mjs`-style checks.
  - **Code snippet rules:** files under `code/`, language tabs, line numbers, Java first.
  - **Verification before every commit:**
    - `npm run lint`, `npm run check`, `npm run check:content`, `npm run diagrams -- --check` and
      `npm run build`;
    - the mobile overflow audit;
    - the offline crawl (every page loads from the service worker with no failed requests).
  - **Offline/PWA architecture:**
    - `scripts/pwa/build-sw.ts` writes `dist/sw.js` with a hash per file; updates download only
      changed files;
    - the update prompt; the `/offline/` page actions;
    - how to test.
  - **Pointer to this backlog** and how to pick the next item.
- Optionally move the useful scripts in `.cache/` (walkthrough check, mobile audit, offline crawl,
  update test) into `scripts/` with npm entries, so they're part of the repo and documented.

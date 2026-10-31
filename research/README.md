# Research: what the guide should cover

The topic list is driven by data, not guesswork. This folder holds the inputs, the method and the resulting decisions.

- **Data snapshot:** 2026-10-30 (LeetCode Discuss posts up to October 2026, job postings from October 2026).
- **Last reviewed:** 2026-11-01. Remaining agreed work, including new tracks and topics, is in [`../BACKLOG.md`](../BACKLOG.md).

## Files

| File | What it holds |
|---|---|
| `topic-matrix.json` | Each research topic with its report count by year, top companies and the keep / add / expand decision |
| `company-questions.json` | Per company: topics reported and links to the posts |
| `job-demand.json` | Skill counts from job-posting aggregators |
| `topic-map.json` | Research keys → guide topic ids; `research:apply` uses it to write `src/content/reports/` |

## Sources

| Source | What it tells us | Files |
|---|---|---|
| **LeetCode Discuss** — 4,430 public, company-tagged interview posts from 68 companies, Jul 2024 → Oct 2026 | Which topics candidates report being asked, by company and year | `topic-matrix.json`, `company-questions.json` |
| **Job-posting aggregators** — freehire (≈294k open backend postings), Skillenai role pages | Which skills employers ask for | `job-demand.json` |
| **Open curricula** — system-design-primer, awesome-low-level-design, awesome-system-design-resources (tables of contents only) | Coverage benchmark for gap-checking; no content copied | — |
| **2026 interview-trend articles** (Design Gurus, Prepfully, Aced, others) | Qualitative changes in interview formats | cited below |

## Method

1. `scripts/research/leetcode-discuss.ts` lists recent posts per company tag through LeetCode's public GraphQL endpoint (sequential, rate-limited) and caches post text locally in `.cache/` (not committed).
2. `scripts/research/analyze.ts` matches each post against about 150 conservative phrase patterns (e.g. "parking lot", "consistent hashing", "sliding window"). A post counts once per topic. Patterns that matched noise were tightened: "timeline" matched process timelines, "test cases" matched every DSA post, and "Netflix" matched the company tag.
3. `research/topic-map.json` maps research keys to guide topics. `scripts/research/apply-reports.ts` writes `src/content/reports/<topic>.yaml`, which the site renders as "Reported in interviews at …" with links to the original posts.

Re-run all three steps (`npm run research:fetch`, `research:analyze`, `research:apply`) to refresh the data, then update the snapshot date above.

**Before writing any new topic** (each content item in the backlog), do a fresh, smaller pass for that subject: web search for current interview reports and job demand, and record every company tag with its source URL (`askedAt` on the topic or question).

Counts are **mentions in candidate reports**, not official question banks, and English-language LeetCode Discuss over-represents India-based and big-tech candidates.

## Findings

Share of posts mentioning a topic (posts by year: 2024 H2 = 1,133; 2025 = 1,868; 2026 Jan–Oct = 1,429):

| Area | Signal |
|---|---|
| **Behavioural / leadership** | 23% of all posts (Amazon, Salesforce, Goldman Sachs, Flipkart, Meta, Uber). Project deep-dives: 5%. |
| **DSA** | Graphs 9.3%, DP 7.9%, grid/matrix 5.4%, binary search 4.7%, heaps 4.7%, trees 4.4%, sliding window 3.1%, stacks 2.8%, greedy 2.6%, backtracking 2.3%, intervals 2.1% |
| **LLD** | Machine coding 5.0% (Flipkart 89, PhonePe 31, Uber 19, Meesho 18, Razorpay 16); design patterns 4.9%; OOP 4.0%; LRU 2.2%; rate limiter 2.0%; shopping cart, task scheduler, **wallet/payments (52)**, movie booking, meeting room, parking lot, notifications |
| **Concurrency** | 3.7% (Walmart, Goldman Sachs, Oracle, LinkedIn, D. E. Shaw) |
| **Backend** | Spring 3.0%, REST API design 2.7%; **idempotency rising** (0 → 4 → 14 posts) |
| **Architecture / data** | Microservices 2.3%, Kafka 2.1%, NoSQL 2.1%, SQL queries 2.0%, caching 1.4%, indexing, sharding, transactions, replication |
| **HLD** | Real-time analytics/top-K, chat, payments, news feed, notifications, search, file storage, proximity, collaborative editing, metrics/logging, booking, URL shortener |
| **AI (fastest growing)** | LLM concepts in interviews: 3 → 18 → 29 posts (≈8× growth in share 2024 → 2026). **AI-assisted coding rounds**: 0 → 12 → 28 (Meta 11, Amazon 10, Atlassian 7). LLM/RAG application design: 0 → 6 → 12. |

Job postings (freehire, Oct 2026): API 22k, **Java 15.7k**, cloud 14.7k, AWS 12.8k, Python 12.3k, **AI 12.2k**, CI/CD 12.1k, REST 11.6k, microservices 10.4k, Docker 10.3k, SQL 9.9k, Kubernetes 9.5k, PostgreSQL 9.3k, Spring 8.2k, distributed systems 6.4k, Kafka 6.2k, observability 5.7k, Redis 5.2k, event-driven 4.8k, LLM 3.0k. Fastest 30-day growth: Python, REST, API, microservices, MySQL, Redis, AWS, Docker, AI.

Qualitative 2026 trends: the system-design bar has risen; **AI-aware designs** (LLMs, vector stores, RAG) now appear in general SWE loops; **cost reasoning** is graded explicitly; and coding rounds are moving towards judgement, debugging and AI-assisted formats ([Design Gurus](https://designgurus.substack.com/p/system-design-interviews-changed), [Prepfully](https://prepfully.com/interview-guides/software-engineer-interview-rubric-2026), [Levelop](https://levelop.dev/blog/the-complete-software-engineer-interview-process-in-2026-what-to-expect-at-every)). Modern Java (17/21 LTS: records, sealed types, virtual threads) is now standard in Java interviews ([techinterview.org](https://www.techinterview.org/post/3233474451/java-interview-questions-2025-virtual-threads-records-sealed-classes-stream-api-garbage-collection-concurrency-spring-boot/)).

## Decisions (2026-10-30)

**Keep** every migrated legacy topic; all of them show up in the data.

**Add a new track — AI Engineering for Backend Developers**:
- LLM fundamentals: tokens, context windows, sampling, embeddings, cost and latency
- RAG end to end: chunking, embeddings, vector search, reranking, evaluation
- Vector databases and ANN indexes (HNSW, IVF)
- LLM application system design: gateway, caching, rate limits, fallbacks, guardrails, observability, cost control
- Agents, tool calling and MCP
- Using AI assistants well (also covered in Interviews: AI-assisted coding rounds)

**Prioritise the DSA track** (Phase 2, first): arrays & hashing, two pointers, sliding window, binary search, stacks & monotonic stacks, linked lists, heaps & top-K, trees & BST, graphs (BFS/DFS on grids too), topological sort, shortest paths, union-find, intervals, backtracking, greedy, dynamic programming, bit manipulation, and design-a-data-structure problems.

**New LLD topics** (by report frequency): digital wallet / payments, LFU cache, inventory management, in-memory pub/sub, logging framework, vending machine, leaderboard, food delivery.

**New HLD topics**: notification system, file storage (Dropbox/S3), video streaming, proximity service (Yelp/nearby), web crawler, distributed cache, ticket booking at scale, ride hailing (HLD), recommendation basics, webhook delivery, and the building blocks consistent hashing, load balancing and CDN as standalone topics.

**Expand**: idempotency (rising), realtime communication (WebSockets/SSE, rising), modern Java 17–25 (records, sealed, pattern matching, virtual threads), observability, cloud fundamentals, cost reasoning in system design.

**Interviews & Career**: behavioural (STAR, leadership principles), project deep-dive, AI-assisted coding rounds, debugging/code-review rounds, how loops differ by company type.

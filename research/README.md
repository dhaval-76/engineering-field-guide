# Research: what the guide should cover

The topic list is driven by data, not guesswork. This folder holds the inputs, the method and the resulting decisions.

- **Data snapshot:** 2026-10-30 (LeetCode Discuss posts up to October 2026, job postings from October 2026).
- **Last reviewed:** 2026-11-09. Remaining agreed work, including new tracks and topics, is in [`../BACKLOG.md`](../BACKLOG.md).

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

## Research log: AI Engineering additions (2026-11-09)

Done before writing the five new `ai-engineering/` topics.

**Interview reports.** Searching the 4,431 cached LeetCode Discuss posts for these subjects found few first-hand reports, and only three were usable as `askedAt` tags, all from 2026: [Google, Gemini Applications hiring manager](https://leetcode.com/discuss/post/7936329/google-hiring-manager-round-gemini-appli-mb5k/) (evaluation), [Google Cloud AI engineer](https://leetcode.com/discuss/post/8583957/recent-google-interview-experience-for-g-gkmp/) (prompt injection) and [Amazon SDE I, GenAI round](https://leetcode.com/discuss/post/8542502/software-development-engineer-i-amazon-i-tusz/) (verifying output, securing input, personal data). A post tagged Google titled "LLM Interview Questions I have Been Preparing" was deliberately **not** used: it is a study list, not a report of being asked. No first-hand reports were found for HNSW, fine-tuning vs RAG or prompt caching, so those pages carry no company tags. Web searches showed these are widely listed as interview topics, but only on prep sites of uneven quality.

**Primary sources for facts** (checked 2026-11-09):

| Topic | Source |
|---|---|
| Prompt injection | [OWASP LLM01](https://genai.owasp.org/llmrisk/llm01-prompt-injection/), including "it is unclear if there are fool-proof methods of prevention" |
| Lethal trifecta | [Simon Willison](https://simonwillison.net/2025/Jun/16/the-lethal-trifecta/) |
| HNSW | [Malkov and Yashunin](https://arxiv.org/abs/1603.09320); [pgvector](https://github.com/pgvector/pgvector) for defaults, filtering and limits |
| Caching | [OpenAI](https://developers.openai.com/api/docs/guides/prompt-caching), [Gemini](https://aistudio.google.com/docs/caching), Anthropic's prompt caching reference |
| Fine-tuning | [OpenAI model optimization](https://developers.openai.com/api/docs/guides/model-optimization): the platform is winding down and closed to new users |
| Evals | [OpenAI evals](https://developers.openai.com/api/docs/guides/evals): the product shuts down on 30 November 2026 |

**Measured, not quoted.** The HNSW recall and distance charts come from running the page's own Java code on 20,000 clustered 64-dimensional vectors with 200 queries. The confidence intervals and cost figures are computed by the code shown on those pages.

## Research log: API styles (2026-11-09)

**Interview reports used as `askedAt` tags**, each read in context: [Microsoft 2026](https://leetcode.com/discuss/post/7636712/selected-microsoft-sde2l61-interview-exp-qekl/), [ServiceNow 2025](https://leetcode.com/discuss/post/6667179/servicenow-no-response-after-last-round-u7hs1/) and [Oracle 2025](https://leetcode.com/discuss/post/6555411/oracle-oci-ic2-interview-experience-by-a-v1lf/) for gRPC and Protobuf; [Walmart 2024](https://leetcode.com/discuss/post/6146111/walmart-sde-3-interview-experience-by-ur-uels/) and [Uber 2025](https://leetcode.com/discuss/post/6820731/uber-sde-2-frontend-by-anonymous_user-rvuq/) for GraphQL; [Goldman Sachs 2025](https://leetcode.com/discuss/post/6891139/goldman-sachs-analyst-interview-experien-3cm8/) for versioning. Not used: a Google-tagged post that is an article about gRPC, and a Shopify post where the candidate raised versioning unprompted.

**Primary sources:** grpc.io guides ([performance](https://grpc.io/docs/guides/performance/), [deadlines](https://grpc.io/docs/guides/deadlines/), [status codes](https://grpc.io/docs/guides/status-codes/), [core concepts](https://grpc.io/docs/what-is-grpc/core-concepts/)), the [gRPC HTTP/2 protocol](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md), Protobuf [encoding](https://protobuf.dev/programming-guides/encoding/) and [proto3](https://protobuf.dev/programming-guides/proto3/), graphql.org ([security](https://graphql.org/learn/security/), [performance](https://graphql.org/learn/performance/), [serving over HTTP](https://graphql.org/learn/serving-over-http/)), the [Relay connection spec](https://relay.dev/graphql/connections.htm), [RFC 8594](https://www.rfc-editor.org/rfc/rfc8594) (Sunset), [RFC 9745](https://www.rfc-editor.org/rfc/rfc9745) (Deprecation), [Google AIP-180](https://google.aip.dev/180) and [Stripe's versioning design](https://stripe.com/blog/api-versioning) (2017).

**Measured:** the payload chart on the gRPC page encodes 100 sample orders with the page's own code and gzip. Web comparisons of gRPC and REST disagree widely on speed, so the page quotes no general speed-up figure.

## Research log: CS Foundations (2026-11-09 to 2026-11-10)

Research for the 9-page Networking and Operating Systems split, written on 2026-11-10. Interview reports, Linux measurements, sources, rejected posts and the facts still to confirm are in [`cs-foundations-2026-11.md`](cs-foundations-2026-11.md). The measurement programs are in [`probes/cs-foundations/`](probes/cs-foundations/).

## Research log: company question banks (2026-11-10)

**Amazon.** Read the 2026 posts in the LeetCode Discuss cache (316 posts). LeetCode's public problem list (4,109 titles) was matched against the text and links to find named problems, then each citation was read in context. That removed problems listed only as practice links, and one candidate who posted the same story three times counts once. Online assessment posts are not used as question sources. [A public compilation](https://leetcode.com/discuss/post/8519174/) is cited only for the few questions with no first-hand report, and those entries say so.

**Cashfree** wasn't in the cache, so 56 posts mentioning it were fetched from LeetCode Discuss on 2026-11-09 (Cashfree is now in the fetch list). 13 contain real questions, from 2021 to 2026, so older ones are included and dated. The other posts are offers or requests for help. Not used: `interviewexperiences.in` (mirrors the same LeetCode posts), a generic scaleengineer.com guide (not tied to any candidate), Glassdoor (blocked), and a search snippet claiming LeetCode 84 and 410 that couldn't be traced to any report.

## Research log: Security track (2026-11-10)

**Interview reports.** The cached LeetCode posts barely mention security by name: no first-hand reports for SQL injection, XSS, CSRF, SSRF or OWASP. Tags used: [Salesforce 2026](https://leetcode.com/discuss/post/7455964/salesforce-smts-interview-experience-rej-meuy/) (how passwords are stored with BCrypt) and [Apple 2025](https://leetcode.com/discuss/post/6979238/my-apple-ic3-frontend-interview-experien-k4jt/) (securing a website: HTTPS, CSP, CORS, cookies). Not used: a Goldman Sachs post about a job simulation, not an interview.

**Primary sources read.** The OWASP Top 10:2025 (final; SSRF folded into A01, A10 new), and the OWASP cheat sheets for password storage (Argon2id 19 MiB / 2 / 1, scrypt 2^17 / 8 / 1, bcrypt cost 10+ with its 72-byte limit, PBKDF2-SHA256 600,000), SSRF, CSRF (Sec-Fetch-Site as the primary signal; SameSite as defence in depth), XSS, SQL injection and secrets management. AWS's IMDSv2 page (token by PUT, hop limit 1, X-Forwarded-For refused), AWS KMS envelope encryption, Stripe's and GitHub's webhook verification docs (GitHub's test vector is used in the tests). Capital One's own breach statement doesn't mention SSRF, so the pages don't use that example. NIST SP 800-38D's page had no nonce guidance in its HTML; the 2^32 random-nonce limit is from the standard itself.

**Testing.** Node's `BlockList` matches IPv4-mapped IPv6 addresses with IPv4 rules; an extra `::ffff:0:0/96` rule wrongly blocked every IPv4 address, so it was removed. Python's `cryptography` package and the SQLite JDBC driver were installed only in throwaway locations for testing.

## Research log: More DevOps (2026-11-10)

**Interview reports** used as tags, each read in context: canary and blue-green at [Walmart 2025](https://leetcode.com/discuss/post/7039649/walmart-software-engineer-iii-interview-b6bqt/), [Salesforce 2025](https://leetcode.com/discuss/post/6297389/salesforce-smts-hyderabad-jan-2025-offer-5aq1/) and [Bloomberg 2025](https://leetcode.com/discuss/post/7347254/bloomberg-interview-experience-senior-so-n2uw/); Terraform at [NVIDIA 2026](https://leetcode.com/discuss/post/8560520/nvidia-platform-engineer-interview-exper-kae0/) (also Terraform vs Helm, drift, observability), [JPMorgan Chase 2026](https://leetcode.com/discuss/post/8518755/senior-data-engineer-software-engineer-i-inc1/), [Salesforce 2025](https://leetcode.com/discuss/post/6732405/salesforce-public-cloud-gcp-interview-fo-o4cu/) and [American Express 2024](https://leetcode.com/discuss/post/5501520/american-express-amex-engineer-1-bengalu-5kkq/). Not used: posts where the candidate raised canary or Terraform themselves.

**Primary sources read:** Kubernetes Deployment docs (25% surge and unavailable, progressDeadlineSeconds 600, revisionHistoryLimit 10), Argo Rollouts canary and analysis docs (abort sets the canary weight to zero), Terraform state, sensitive data (secrets stored in plain text; `sensitive` only hides output), locking and `plan -detailed-exitcode`, the Google SRE Workbook's burn-rate table, Prometheus histogram and naming practices, Kubernetes logging docs (summarised by the fetch tool, not verbatim) and the kube-state-metrics README. The OpenTelemetry collector deployment page returned 404, so the page describes agent and gateway only in general terms.

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

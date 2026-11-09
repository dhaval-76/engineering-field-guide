# CS Foundations research notes (2026-11-09)

Research for backlog item 1: splitting Networking and Operating Systems into 9 full pages. **Paused
at the research stage at the owner's request.** No topic has been written and nothing under
`src/content` has changed. Start here when the item resumes.

## Where this stands

| Step | State |
|---|---|
| Interview reports searched and read | Done |
| Measurements on Linux | Done, except TIME_WAIT |
| Web search for current facts | Done, mostly secondary sources |
| Official docs check | **Not done.** It was stopped before returning anything; list below |
| Writing the pages | Not started |

## Planned pages

The current URLs stay as the first page of each group, as the backlog asks.

| Page | Slug | Order |
|---|---|---|
| How a request travels | `cs-foundations/networking` (existing) | 20 |
| TCP and UDP | `cs-foundations/tcp-and-udp` | 21 |
| HTTP/1.1, HTTP/2 and HTTP/3 | `cs-foundations/http-versions` | 22 |
| DNS and TLS | `cs-foundations/dns-and-tls` | 23 |
| Real-time connections | `cs-foundations/realtime-connections` | 24 |
| Processes and threads | `cs-foundations/operating-systems` (existing) | 30 |
| Memory | `cs-foundations/memory-management` | 31 |
| Files and I/O | `cs-foundations/files-and-io` | 32 |
| Containers in OS terms | `cs-foundations/containers-in-os-terms` | 33 |

Slugs and order numbers are proposals.

**Decisions proposed, not yet confirmed by the owner:**

- **Containers page:** keep it in CS Foundations and link to it from DevOps, because it explains
  namespaces and cgroups as OS features. This is the backlog's open question.
- **Real-time page:** `backend/realtime-communication` already compares polling, SSE and WebSockets
  and covers scaling. Don't repeat that. The new page covers the connection itself (upgrade
  handshake, frames) and how proxies and load balancers affect each option, which the backend page
  barely touches.

**Links to repoint when the pages exist:**

- Glossary `topic:` fields: `tcp` and `head-of-line-blocking` (TCP page; check the second against
  the HTTP page), `tls` and `dns` (DNS and TLS), `http2` (HTTP versions). `context-switch` already
  points at `operating-systems`, which stays correct.
- `backend/realtime-communication`: its `ulimit -n` link should go to Files and I/O.
- `devops/docker-kubernetes`: link to the containers page.
- `system-design/messaging-system`: link to Files and I/O for zero-copy.
- `concurrency/volatile-and-synchronized` or a similar page: link to Memory for false sharing.
- `research/topic-map.json` maps `concept-networking` and `concept-os` to the first page only, so
  the generated "Reported in interviews" data stays there unless the map is split.

## Interview reports

Searched the 4,431 cached LeetCode Discuss posts. Each post below was read in context. Check the
company against the text, not the tag: the tag was wrong or too broad in several posts.

**How a request travels** ("what happens when you type a URL")

- [Zomato 2025](https://leetcode.com/discuss/post/6493240/zomato-interview-experience-sde-off-camp-rt67/)
- [Zomato (Eternal) 2025](https://leetcode.com/discuss/post/6994052/eternal-ltdzomato-sde-1-interview-experi-fbnm/)
- [Paytm 2026](https://leetcode.com/discuss/post/8548376/paytm-interview-experience-oncampus-by-b-m141/)
- [Google 2026, Web Solutions L3](https://leetcode.com/discuss/post/7624355/interview-experience-google-l3-web-solut-ger7/)
- [Oracle 2025](https://leetcode.com/discuss/post/6401040/oracle-gbs-sde-1-2024-on-campus-india-in-rti1/)
- [Walmart 2025](https://leetcode.com/discuss/post/6837159/walmart-sde-intern-fte-interview-experie-8j8k/)
- [Flipkart 2025, UI Engineer](https://leetcode.com/discuss/post/6909362/flipkart-ui-engineer-2-bangalore-july-20-6rlz/)
- [Walmart 2024](https://leetcode.com/discuss/post/5751077/walmart-sde-ii-bengaluru-september-2024-hx68m/): what happens when you message a friend on WhatsApp, layer by layer
- [Zeta 2025](https://leetcode.com/discuss/post/6357165/zeta-directi-interview-experience-for-sd-zne2/)

**TCP and UDP**

- [Swiggy 2025](https://leetcode.com/discuss/post/6256994/swiggy-asde-online-interviews-december-2-8l2y/): TCP vs UDP, use cases, TCP vs HTTP
- [Oracle Cloud 2026](https://leetcode.com/discuss/post/8477884/interview-summary-of-last-6-months-micro-y7a7/): how ping works, TCP vs UDP. The post covers several companies and is tagged Microsoft; this question was at **Oracle**
- [Goldman Sachs 2025](https://leetcode.com/discuss/post/6221534/goldman-sachs-associate-bangalore-dec-20-7qcs/): HTTP vs HTTPS, TCP vs UDP
- [Meesho 2025](https://leetcode.com/discuss/post/6834461/meesho-sde-1-by-anonymous_user-9iuh/)
- [Zscaler 2024](https://leetcode.com/discuss/post/5801403/zscaler-intern-fte-oncampus-2023-by-anon-m8ao/): three-way handshake extended to the TLS handshake, TCP vs UDP

**HTTP (CORS and caching)**

- [Razorpay 2025](https://leetcode.com/discuss/post/6273899/razorpay-frontend-interview-experience-b-yvc8/): the OPTIONS method, CORS errors, same-origin policy
- [Oracle 2025](https://leetcode.com/discuss/post/6912254/oracle-l4-phone-screen-experience-by-ano-0dcx/): OPTIONS and CORS preflight
- [Apple 2025](https://leetcode.com/discuss/post/6979238/my-apple-ic3-frontend-interview-experien-k4jt/): HTTPS, CSP, CORS, securing cookies
- [TikTok 2024](https://leetcode.com/discuss/post/5739388/tiktok-oa-by-anonymous_user-nq1i/): allowing a cross-origin request
- [Walmart 2024](https://leetcode.com/discuss/post/5505527/walmart-sde-3-bangalore-by-anonymous_use-9nwa/): CORS, among Spring questions
- [PayPal 2025](https://leetcode.com/discuss/post/7295033/sde-3python-paypal-interview-experience-xvp23/): 401 vs 403 and the cache-control header (multiple choice)

No first-hand reports asked about HTTP/2 or HTTP/3 by name.

**DNS and TLS**

- [Oracle 2025](https://leetcode.com/discuss/post/6555411/oracle-oci-ic2-interview-experience-by-a-v1lf/): how HTTPS works
- [Zscaler 2024](https://leetcode.com/discuss/post/5801403/zscaler-intern-fte-oncampus-2023-by-anon-m8ao/): the TLS handshake
- [Atlassian 2024](https://leetcode.com/discuss/post/5677733/atlassian-virtual-onsite-2-rounds-p60-cl-ea8x/): DNS resolution, how certificates are used with HTTPS, TLS termination at the load balancer
- [Salesforce 2026](https://leetcode.com/discuss/post/8204135/salesforce-amts-interview-should-i-move-wkg4g/): what a DNS server is, how to see which server and IP a request reached

**Real-time connections**

- [Adobe 2025](https://leetcode.com/discuss/post/6938276/adobe-mts-2-interview-experience-fe-by-a-5h33/): polling vs WebSocket trade-offs

**Processes and threads**

- [LinkedIn 2026](https://leetcode.com/discuss/post/7551445/linkedin-swe-ic2-infra-phone-screening-b-z2zw/)
- [LinkedIn 2026](https://leetcode.com/discuss/post/7571938/interview-experience-linkedin-swe-apps-2-pguc/): how an app boots, tied to processes vs threads; how many threads a multi-core machine can run
- [Microsoft 2026](https://leetcode.com/discuss/post/7636712/selected-microsoft-sde2l61-interview-exp-qekl/): process vs thread, heap variables vs local variables across threads
- [Intuit 2026](https://leetcode.com/discuss/post/8502025/intuit-swe-intern-2026-oncampus-intervie-keaj/): scheduling; why a crashed Chrome tab doesn't take down the rest
- [Paytm 2026](https://leetcode.com/discuss/post/8548376/paytm-interview-experience-oncampus-by-b-m141/)
- [Swiggy 2025](https://leetcode.com/discuss/post/6256994/swiggy-asde-online-interviews-december-2-8l2y/)
- [LinkedIn 2025](https://leetcode.com/discuss/post/6284165/linked-interview-experience-swe-systems-qvesb/)
- [NVIDIA 2026](https://leetcode.com/discuss/post/8510550/nvidia-interview-experience-senior-syste-vr87/): context switching, paging

**Memory**

- [Google 2026, Web Solutions L3](https://leetcode.com/discuss/post/7611418/google-web-solutions-engineer-l3-by-anon-fo7v/): virtual memory, how the CPU handles many tasks
- [Microsoft Azure 2026](https://leetcode.com/discuss/post/8477884/interview-summary-of-last-6-months-micro-y7a7/): paging, virtual memory
- [LinkedIn 2025](https://leetcode.com/discuss/post/6487244/linkedin-sse-bangalore-rejected-by-anony-dlq3/): paging, thrashing, segmentation, page faults
- [LinkedIn 2025](https://leetcode.com/discuss/post/6713433/sse-infrastructure-linkedin-april-2025-b-itjf/): paging, thrashing
- [Adobe 2025](https://leetcode.com/discuss/post/6603811/sde-2-c-developer-at-adobe-interview-exp-376m/): paging
- [Adobe 2025](https://leetcode.com/discuss/post/6722120/adobe-computer-scientist-1-round-1-inter-xjos/): virtual memory, paging and segmentation
- [LinkedIn 2025](https://leetcode.com/discuss/post/7346832/linkedin-swe-infra-track-interviews-reje-nh70/): stack vs heap
- [Meesho 2025](https://leetcode.com/discuss/post/6630553/meesho-sde-1-interview-experience-by-ano-26t3/): stack and heap, memory management

**Files and I/O:** no reports for epoll, io_uring, zero-copy, fsync, file descriptors or the page
cache. This page gets no company tags.

**Containers**

- [D. E. Shaw 2025](https://leetcode.com/discuss/post/6686275/de-shaw-smts-application-engineer-by-ano-frwl/): containers vs VMs
- [Salesforce 2026](https://leetcode.com/discuss/post/8204135/salesforce-amts-interview-should-i-move-wkg4g/): containers, deploying to Kubernetes

No reports asked about cgroups or CPU throttling by name.

**Rejected, so don't tag these:**

| Post | Why |
|---|---|
| [7338371](https://leetcode.com/discuss/post/7338371/essential-cs-fundamentals-topics-for-int-38ni/) (Oracle tag) | A study list, not a report |
| [7356784](https://leetcode.com/discuss/post/7356784/i-failed-my-first-interview-because-i-sk-bcpv/) (Microsoft tag) | Prep advice; it doesn't say which company asked what |
| [7398769](https://leetcode.com/discuss/post/7398769/jp-morgan-sde-2025-interview-prep-must-s-9bgf/) (JPMorgan tag) | A prep sheet, not a first-hand report |
| [8586085](https://leetcode.com/discuss/post/8586085/zomato-feeding-india-sde-1-interview-exp-m5ym/) (Zomato tag) | "You should also be comfortable with", which is advice, not a question asked |
| [7299290](https://leetcode.com/discuss/post/7299290/roadway-to-success-by-anonymous_user-07kd/) (Salesforce tag) | The handshake question was at Zeta, in a single-line summary; use only if tagged Zeta |
| [6229468](https://leetcode.com/discuss/post/6229468/goldman-sachs-associate-bangalore-by-ano-csv2/) (Flipkart tag) | The post is about Goldman Sachs; the tag is wrong |
| [7529773](https://leetcode.com/discuss/post/7529773/cors-in-system-design-why-your-api-works-155d/), [7521173](https://leetcode.com/discuss/post/7521173/grpc-explained-like-you-actually-need-it-4ci6/) (Google tag) | Articles, not reports |

## Measurements

Environment: Docker Desktop on an Apple Silicon Mac, Linux 6.12.67 (linuxkit), 8 CPUs, 3.9 GB,
cgroup v2, Temurin 21.0.8 from the `apache/kafka:4.1.0` image. These are one laptop's numbers, so
use the ratios and the shape, not the absolute values. The programs are in
[`probes/cs-foundations/`](probes/cs-foundations/).

| What | Result | Note for the page |
|---|---|---|
| Write, write, read on loopback, Nagle on | median 42 ms | Nagle holds the second write and the receiver delays its ACK |
| Same with `TCP_NODELAY` | median 0.1 ms | |
| 10,000 platform threads started | about 1 s | Second, warmed round |
| 10,000 virtual threads started | 2 ms | Second round; the first took 720 ms, mostly class loading |
| One-way handoff between two platform threads | about 15 µs | `SynchronousQueue`, so it includes the wake-up, not a pure context switch |
| Two counters on one cache line vs padded apart, 200 M increments each | 5.8 s vs 0.9 s | False sharing, about 6 times slower |
| Reading 64 M ints, in order vs shuffled | 0.6 ns vs 4 ns per read | 256 MB array; the shuffled time includes reading the index array |
| 4 KB write without fsync | 23 µs | |
| 4 KB write with fsync | 744 µs (about 1,345 per second) | Virtual disk on a laptop; real disks differ |
| Reading 1 GB after dropping caches | about 720 ms | |
| Reading it again from the page cache | 88 to 277 ms | Noisy |
| Sending 1 GB over loopback, read and write | about 230 ms | Warmed rounds |
| Same with `transferTo` (sendfile) | about 49 ms | Loopback only; a real network is usually the limit |

**CPU limits (`--cpus`), a busy loop reporting gaps over 1 ms, 3 seconds:**

| Limit and threads | Gaps | Typical gap | Longest | Reading |
|---|---|---|---|---|
| No limit, 1 thread | 0 | | | |
| 1 CPU, 1 thread | 9 | 4 ms | 6.5 ms | Quota covers the work |
| 0.5 CPU, 1 thread | 30 | 50 ms | 60 ms | Runs 50 ms of each 100 ms period, once per period |
| 1 CPU, 4 threads | 121 | 75 ms | 84 ms | 4 threads spend 100 ms of quota in 25 ms, then stall 75 ms |

That last row is the page's main point: a limit of 1 CPU doesn't mean one thread at a time. A busy
multi-threaded process stalls for most of every period.

**The JVM in a container** (default collector noted where it mattered):

| Container limits | `availableProcessors` | Max heap |
|---|---|---|
| None (the whole 3.9 GB VM) | 8 | 980 MB |
| 1 GB, 2 CPUs | 2 | 247 MB |
| 1 GB, 0.5 CPU | 1 | 247 MB |
| 1 GB, CPU shares only | 8 | 247 MB |

- `MaxRAMPercentage` is 25 and `UseContainerSupport` is true by default.
- At a 1 GB limit the JVM picked SerialGC, not G1 (`UseSerialGC ... {ergonomic}`).
- CPU shares don't change the processor count, matching
  [JDK-8281571](https://bugs.openjdk.org/browse/JDK-8281571).

**Running out of memory, 512 MB limit:**

| Heap | What happened |
|---|---|
| Default | Java `OutOfMemoryError` after 122 MB, which the program can catch; exit 0 |
| `-Xmx1g`, above the limit | Killed by the kernel with no Java error; exit 137 |

**TCP settings read from the container:** ephemeral ports 32768 to 60999, `tcp_tw_reuse` = 2.

**Unresolved:** the TIME_WAIT probe found 0 sockets at every check after 2,000 closed connections.
That is almost certainly a flaw in the probe, cause not found. Don't publish a TIME_WAIT
measurement from it.

Probes that gave no usable result: `timewait` and `pagecache` in `M.java`. `P.java` holds the
corrected page cache test (needs `--user root --privileged`).

## Facts from web search

Mostly secondary sources (blogs and vendor guides). Confirm against the primary source before using
any of these on a page.

**TCP.** Linux holds TIME_WAIT for about 60 seconds, fixed in the kernel. `tcp_fin_timeout` affects
orphaned FIN_WAIT2 sockets and doesn't shorten TIME_WAIT. `tcp_tw_reuse` applies to outbound
connections and needs timestamps. `tcp_tw_recycle` was removed in Linux 4.12. Fix connection churn
first with keep-alive and pools.
([Bernat](https://vincent.bernat.ch/en/blog/2014-tcp-time-wait-state-linux))

**HTTP/3 adoption depends on what is measured.** About 39.5% of sites advertised it in June 2026,
while Cloudflare Radar showed about 21% of requests in Q1 2026, with HTTP/2 at about 51%. One scan
found only 2.3% support on origin servers without a CDN.
([analysis](https://technologychecker.io/blog/http-protocol-adoption),
[W3Techs](https://w3techs.com/technologies/breakdown/ce-http3/content_delivery),
[scan](https://enterno.io/en/s/research-http3-quic-adoption-2026))

**cgroups and the JVM.** `cpu.max` is `quota period`, and the default period is 100 ms. A throttled
group waits for the next period. cgroup v2 support: OpenJDK 17, 11.0.16 and 8u372.
([kernel CFS docs](https://docs.kernel.org/scheduler/sched-bwc.html),
[Red Hat](https://developers.redhat.com/articles/2023/04/19/openjdk-8u372-feature-cgroup-v2-support))

**epoll and io_uring.** Level-triggered is epoll's default. With edge-triggered, use non-blocking
descriptors and read until `EAGAIN`. Google restricted io_uring over security bugs, Docker's default
seccomp profile blocks it, and `kernel.io_uring_disabled` exists from Linux 6.6.
([epoll(7)](https://man7.org/linux/man-pages/man7/epoll.7.html),
[Docker seccomp](https://docs.docker.com/engine/security/seccomp/),
[moby#47532](https://github-redirect.dependabot.com/moby/moby/issues/47532))

**Kafka zero-copy.** Kafka's zero-copy path uses sendfile. TLS turns it off, because the SSL layer
doesn't implement it. ([KAFKA-2517](https://issues.apache.org/jira/browse/KAFKA-2517))

**Proxies.** ALB idle timeout and nginx `proxy_read_timeout` both default to 60 seconds. SSE needs
buffering off (`proxy_buffering off` or `X-Accel-Buffering: no`). Sources disagree on ALB's maximum.
One source reports Cloudflare cutting idle SSE streams at about 125 seconds.
([websocket.org on ALB](https://websocket.org/guides/infrastructure/aws/alb/),
[nginx SSE guide](https://oneuptime.com/blog/post/2025-12-16-server-sent-events-nginx/view),
[Cloudflare report](https://levelup.gitconnected.com/the-125-second-silence-why-your-ai-stream-dies-behind-cloudflare-40f776d9e055))

**CORS.** The Fetch standard defines it and doesn't use the term "simple request". A preflight is an
OPTIONS request with `Access-Control-Request-Method` and `Access-Control-Request-Headers`.
`Access-Control-Max-Age` caches the result, with browser caps that sources report inconsistently.
([MDN](https://developer.mozilla.org/en-US/docs/Glossary/preflight_request),
[HTTP Toolkit](https://httptoolkit.tech/blog/cache-your-cors))

## Still to check against official docs

This check was stopped before it returned anything. Nothing here has been confirmed.

1. [nginx proxy module](https://nginx.org/en/docs/http/ngx_http_proxy_module.html): defaults for
   `proxy_read_timeout`, `proxy_buffering`, `proxy_http_version`; `X-Accel-Buffering`.
2. [nginx WebSocket](https://nginx.org/en/docs/http/websocket.html): required headers and the
   timeout note.
3. [ALB attributes](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/edit-load-balancer-attributes.html):
   idle timeout default and range.
4. [NLB idle timeout](https://docs.aws.amazon.com/elasticloadbalancing/latest/network/update-idle-timeout.html):
   TCP and UDP defaults and ranges.
5. [MDN Access-Control-Max-Age](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Access-Control-Max-Age):
   browser caps and the default.
6. [MDN Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie):
   SameSite defaults and browser differences.
7. [MDN CORS guide](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS): exact conditions
   for skipping a preflight.
8. [Kafka design docs](https://kafka.apache.org/documentation/#maximizingefficiency): sendfile and TLS.
9. [Docker seccomp](https://docs.docker.com/engine/security/seccomp/): whether io_uring is blocked.
10. [RFC 8446](https://www.rfc-editor.org/rfc/rfc8446.html) section 2.3 and appendix E.5: 0-RTT replay.
11. [CA/B Forum SC-081v3](https://cabforum.org/2025/04/11/ballot-sc081v3-introduce-schedule-of-reducing-validity-and-data-reuse-periods/):
    the schedule of certificate lifetimes.
12. [RFC 6298](https://www.rfc-editor.org/rfc/rfc6298.html) initial RTO and
    [RFC 6928](https://www.rfc-editor.org/rfc/rfc6928.html) initial window.
13. [fsync(2)](https://man7.org/linux/man-pages/man2/fsync.2.html): syncing the containing directory.
14. [cgroup v2](https://docs.kernel.org/admin-guide/cgroup-v2.html): `cpu.max` format and default;
    `memory.high` vs `memory.max`.

Also still to do: the SameSite default and RFC 9111 (`no-cache` vs `no-store`, `ETag`, 304), which
the web search didn't cover, and a sound TIME_WAIT measurement.

## Ideas for code and walkthroughs

Not built yet. Each snippet must run in every language shown, with matching results.

| Page | Code | Walkthroughs and visuals |
|---|---|---|
| Request travels | Raw HTTP request over a socket | Keep `request-journey` and `https-handshakes`; chart of round-trip cost by distance |
| TCP and UDP | Length-prefix framing (TCP is a byte stream); socket options | Handshake; lost packet and retransmit; Nagle chart |
| HTTP versions | ETag and 304 server; "does this need a preflight?", checked against real Chromium | Multiplexing and head-of-line blocking |
| DNS and TLS | DNS query built by hand; certificate expiry check | DNS lookup; TLS 1.3 handshake |
| Real-time | WebSocket accept key and frames, checked against the RFC 6455 example; nginx config | How each proxy setting breaks each option |
| Processes | Graceful shutdown on SIGTERM in all 3 languages | Thread start and handoff numbers |
| Memory | In-order vs shuffled reads; false sharing | Existing `virtual-memory`; measured charts |
| Files and I/O | Durable write (temp file, fsync, rename, fsync directory); event loop; `transferTo` | epoll event loop; fsync chart |
| Containers | Reading the cgroup limits | The 100 ms throttling period; heap and OOM tables |

`nginx` isn't in `codeLanguages`, so a config snippet needs a new language entry or the `text` type.

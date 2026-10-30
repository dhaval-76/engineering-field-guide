/**
 * Analyses the cached interview-experience posts (see leetcode-discuss.ts) and writes:
 *   research/topic-matrix.json      per topic: how often it is reported, by whom, and how recently
 *   research/company-questions.json per company+topic: the posts that report it (source URLs)
 *
 * Matching is keyword-based and deliberately conservative: patterns are specific phrases
 * ("parking lot", "consistent hashing"), not generic words ("cache", "design").
 *   npx tsx scripts/research/analyze.ts
 */
import { readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const root = new URL('../../', import.meta.url).pathname;
const cacheDir = join(root, '.cache/research/leetcode');

type Category =
  | 'lld'
  | 'hld'
  | 'dsa'
  | 'concurrency'
  | 'java'
  | 'backend'
  | 'database'
  | 'architecture'
  | 'ai'
  | 'cs'
  | 'devops'
  | 'behavioral';
interface TopicDef {
  key: string;
  label: string;
  category: Category;
  re: RegExp;
}

const t = (key: string, label: string, category: Category, re: RegExp): TopicDef => ({
  key,
  label,
  category,
  re,
});

export const TOPICS: TopicDef[] = [
  // ── Low-level design problems
  t('lld-parking-lot', 'Parking lot', 'lld', /parking\s*(lot|garage|system)/i),
  t('lld-lru-cache', 'LRU cache', 'lld', /\blru\b|least[- ]recently[- ]used/i),
  t('lld-lfu-cache', 'LFU cache', 'lld', /\blfu\b/i),
  t('lld-elevator', 'Elevator / lift system', 'lld', /elevator|\blift system/i),
  t('lld-vending-machine', 'Vending machine', 'lld', /vending\s*machine/i),
  t('lld-chess', 'Chess', 'lld', /\bchess\b/i),
  t('lld-tic-tac-toe', 'Tic-tac-toe', 'lld', /tic[- ]?tac[- ]?toe/i),
  t('lld-snake-ladder', 'Snake and ladder', 'lld', /snakes?\s*(and|&|n)\s*ladders?/i),
  t('lld-snake-game', 'Snake game', 'lld', /snake\s*game/i),
  t('lld-splitwise', 'Splitwise / expense sharing', 'lld', /splitwise|expense\s*shar/i),
  t(
    'lld-ride-sharing',
    'Ride sharing (Uber/Ola)',
    'lld',
    /ride[- ]?(sharing|hailing|booking)|cab\s*booking|design\s+(uber|ola|lyft)\b/i,
  ),
  t(
    'lld-movie-booking',
    'Movie / ticket booking (BookMyShow)',
    'lld',
    /bookmyshow|movie\s*(ticket|booking)|ticket\s*booking/i,
  ),
  t(
    'lld-meeting-room',
    'Meeting room / calendar booking',
    'lld',
    /meeting\s*(room|scheduler)|conference\s*room|calendar\s*(app|system|booking)/i,
  ),
  t(
    'lld-hotel-booking',
    'Hotel / Airbnb booking',
    'lld',
    /hotel\s*(booking|management|reservation)/i,
  ),
  t('lld-library', 'Library management', 'lld', /library\s*management/i),
  t('lld-atm', 'ATM', 'lld', /\batm\b/i),
  t('lld-rate-limiter', 'Rate limiter', 'lld', /rate[- ]?limit/i),
  t('lld-logger', 'Logging framework', 'lld', /logg(er|ing)\s*(framework|library|system)/i),
  t(
    'lld-pub-sub',
    'Pub/sub or message queue (in-memory)',
    'lld',
    /pub[- ]?sub|message\s*queue|kafka[- ]like/i,
  ),
  t('lld-task-scheduler', 'Task / job scheduler', 'lld', /(task|job)\s*scheduler|cron/i),
  t('lld-notification', 'Notification service', 'lld', /notification\s*(service|system)/i),
  t(
    'lld-shopping-cart',
    'Shopping cart / e-commerce',
    'lld',
    /shopping\s*cart|e-?commerce|online\s*shopping/i,
  ),
  t(
    'lld-food-delivery',
    'Food delivery (Swiggy/Zomato)',
    'lld',
    /food\s*(delivery|ordering)|design\s+(swiggy|zomato|doordash)/i,
  ),
  t(
    'lld-inventory',
    'Inventory / warehouse management',
    'lld',
    /inventory\s*(management|system)|warehouse/i,
  ),
  t('lld-text-editor', 'Text editor / undo-redo', 'lld', /text\s*editor|undo[- ]?redo/i),
  t('lld-spreadsheet', 'Spreadsheet / Excel', 'lld', /spreadsheet|\bexcel\b/i),
  t(
    'lld-file-system',
    'File system / unix find',
    'lld',
    /file\s*system|unix\s*find|\bfind\s+command/i,
  ),
  t('lld-autocomplete', 'Autocomplete / typeahead', 'lld', /auto-?complete|type-?ahead/i),
  t(
    'lld-key-value-store',
    'Key-value store / in-memory DB',
    'lld',
    /key[- ]value\s*store|in-?memory\s*(db|database|store)/i,
  ),
  t('lld-leaderboard', 'Leaderboard', 'lld', /leader\s*board/i),
  t(
    'lld-wallet',
    'Digital wallet / payments',
    'lld',
    /(digital|e-?)\s*wallet|payment\s*(system|gateway|service)/i,
  ),
  t(
    'lld-stock-exchange',
    'Stock exchange / order matching',
    'lld',
    /stock\s*(exchange|broker|trading)|order\s*(book|matching)/i,
  ),
  t('lld-coupon', 'Coupons / offers engine', 'lld', /coupon|voucher|offers?\s*engine/i),
  t('lld-car-rental', 'Car / vehicle rental', 'lld', /(car|vehicle|bike)\s*rental/i),
  t('lld-auction', 'Online auction', 'lld', /auction/i),
  t('lld-traffic-signal', 'Traffic signal', 'lld', /traffic\s*(signal|light)/i),
  t('lld-stack-overflow', 'Q&A site (Stack Overflow)', 'lld', /stack\s*overflow/i),
  t('lld-cricinfo', 'Cricket scoreboard (Cricbuzz)', 'lld', /cricbuzz|cricinfo|cricket\s*score/i),
  t(
    'lld-music-streaming',
    'Music streaming (Spotify)',
    'lld',
    /spotify|music\s*(player|streaming)/i,
  ),
  t('lld-workflow', 'Workflow / rules engine', 'lld', /workflow\s*engine|rules?\s*engine/i),
  t('lld-feature-flags', 'Feature flags', 'lld', /feature\s*(flag|toggle)/i),
  t('lld-circuit-breaker', 'Circuit breaker', 'lld', /circuit\s*breaker/i),
  t('lld-connection-pool', 'Connection pool', 'lld', /connection\s*pool/i),
  t('lld-thread-pool', 'Thread pool / executor', 'lld', /thread\s*pool|executor\s*service/i),
  t(
    'lld-blocking-queue',
    'Blocking queue / producer-consumer',
    'lld',
    /blocking\s*queue|producer[- ]consumer/i,
  ),
  t('lld-machine-coding', 'Machine coding round', 'lld', /machine\s*coding/i),

  // ── High-level system design
  t('hld-url-shortener', 'URL shortener', 'hld', /url\s*shorten|tiny\s*url|bit\.?ly/i),
  t(
    'hld-chat',
    'Chat / messaging (WhatsApp)',
    'hld',
    /whats\s*app|chat\s*(app|system|service|application)|messaging\s*(app|system|service)|messenger/i,
  ),
  t(
    'hld-news-feed',
    'News feed / timeline',
    'hld',
    /news\s*feed|timeline|design\s+(twitter|instagram|facebook)/i,
  ),
  t(
    'hld-video-streaming',
    'Video streaming (YouTube/Netflix)',
    'hld',
    /youtube|netflix|video\s*(streaming|platform)/i,
  ),
  t(
    'hld-file-storage',
    'File storage / sync (Dropbox, S3)',
    'hld',
    /dropbox|google\s*drive|\bs3\b|blob\s*storage|file\s*(sharing|storage|sync)/i,
  ),
  t('hld-web-crawler', 'Web crawler', 'hld', /web\s*crawler|crawler/i),
  t(
    'hld-search',
    'Search engine / search service',
    'hld',
    /search\s*(engine|service|system)|elasticsearch|inverted\s*index/i,
  ),
  t(
    'hld-distributed-cache',
    'Distributed cache',
    'hld',
    /distributed\s*cache|design\s+(redis|memcached)/i,
  ),
  t(
    'hld-kv-store',
    'Distributed key-value store',
    'hld',
    /distributed\s*(key[- ]value|kv)|design\s+dynamo/i,
  ),
  t(
    'hld-message-queue',
    'Distributed message queue (Kafka)',
    'hld',
    /design\s+(a\s+)?kafka|distributed\s*(message\s*)?queue/i,
  ),
  t(
    'hld-job-scheduler',
    'Distributed job scheduler',
    'hld',
    /distributed\s*(job|task)\s*scheduler/i,
  ),
  t(
    'hld-payment',
    'Payment system',
    'hld',
    /payment\s*(system|gateway|service|processing)|\bupi\b/i,
  ),
  t(
    'hld-ecommerce',
    'E-commerce / order management',
    'hld',
    /order\s*(management|tracking|service)|checkout\s*(system|flow)/i,
  ),
  t(
    'hld-booking',
    'Booking / reservation at scale',
    'hld',
    /(flight|hotel|ticket)\s*(booking|reservation)\s*(system|platform)/i,
  ),
  t(
    'hld-location',
    'Location / proximity (Yelp, nearby)',
    'hld',
    /proximity|nearby|geo-?hash|quad\s*tree|yelp|google\s*maps/i,
  ),
  t(
    'hld-ride-hailing',
    'Ride hailing at scale (Uber)',
    'hld',
    /design\s+uber|uber\s*(backend|system)|driver\s*matching/i,
  ),
  t(
    'hld-notification',
    'Notification system at scale',
    'hld',
    /notification\s*(system|service)\s*(at\s*scale)?/i,
  ),
  t(
    'hld-collab-editing',
    'Collaborative editing (Google Docs)',
    'hld',
    /google\s*docs|collaborative\s*edit|real[- ]time\s*collab/i,
  ),
  t(
    'hld-metrics-logging',
    'Metrics / logging / monitoring',
    'hld',
    /(metrics|logging|monitoring)\s*(system|platform|service)|observability\s*platform/i,
  ),
  t(
    'hld-analytics',
    'Real-time analytics / counting',
    'hld',
    /real[- ]time\s*analytics|ad\s*click|top[- ]?k|view\s*count/i,
  ),
  t('hld-id-generator', 'Unique ID generator', 'hld', /unique\s*id|snowflake\s*id|id\s*generat/i),
  t('hld-api-gateway', 'API gateway', 'hld', /api\s*gateway/i),
  t(
    'hld-auth',
    'Authentication / SSO system',
    'hld',
    /(auth|authentication|login|sso)\s*(system|service)/i,
  ),
  t('hld-pastebin', 'Pastebin', 'hld', /pastebin/i),
  t('hld-leaderboard', 'Real-time leaderboard', 'hld', /real[- ]time\s*leader\s*board/i),
  t(
    'hld-recommendation',
    'Recommendation system',
    'hld',
    /recommendation\s*(system|engine|service)/i,
  ),
  t(
    'hld-ad-system',
    'Ads / ad delivery',
    'hld',
    /ad\s*(delivery|serving|system)|ads\s*(platform|system)/i,
  ),
  t('hld-distributed-lock', 'Distributed lock', 'hld', /distributed\s*lock/i),
  t('hld-webhook', 'Webhook delivery', 'hld', /webhook/i),
  t(
    'hld-llm-app',
    'LLM / RAG application',
    'ai',
    /\brag\b|retrieval[- ]augmented|llm\s*(app|application|system|service|gateway)|chat\s*gpt\s*like|chatbot/i,
  ),

  // ── Concepts
  t('concept-consistent-hashing', 'Consistent hashing', 'architecture', /consistent\s*hashing/i),
  t(
    'concept-cap',
    'CAP / consistency models',
    'architecture',
    /\bcap\s*theorem|eventual\s*consistency|strong\s*consistency/i,
  ),
  t(
    'concept-sharding',
    'Sharding / partitioning',
    'database',
    /shard|partition(ing)?\s*(key|strategy|data)/i,
  ),
  t('concept-replication', 'Replication', 'database', /replication|read\s*replica/i),
  t(
    'concept-indexing',
    'Database indexing',
    'database',
    /\bindex(es|ing)?\b.*\b(b-?tree|database|sql|query)|b\+?\s*tree/i,
  ),
  t(
    'concept-transactions',
    'Transactions / isolation levels',
    'database',
    /isolation\s*level|acid|transaction\s*isolation|dirty\s*read|phantom/i,
  ),
  t(
    'concept-sql-queries',
    'SQL queries / joins / window functions',
    'database',
    /\bsql\s*(query|queries|round)|window\s*function|\bjoins?\b.*\bsql|nth\s*highest\s*salary/i,
  ),
  t(
    'concept-nosql',
    'SQL vs NoSQL / MongoDB / Cassandra',
    'database',
    /nosql|mongo\s*db|cassandra|dynamo\s*db/i,
  ),
  t(
    'concept-caching',
    'Caching strategies / Redis',
    'database',
    /\bredis\b|cache[- ]aside|write[- ]through|write[- ]back|cache\s*invalidation/i,
  ),
  t(
    'concept-kafka',
    'Kafka / message brokers',
    'architecture',
    /\bkafka\b|rabbit\s*mq|message\s*broker/i,
  ),
  t('concept-microservices', 'Microservices', 'architecture', /microservice/i),
  t(
    'concept-saga',
    'Saga / distributed transactions',
    'architecture',
    /\bsaga\b|two[- ]phase\s*commit|\b2pc\b|outbox/i,
  ),
  t(
    'concept-event-driven',
    'Event-driven / CQRS / event sourcing',
    'architecture',
    /event[- ]driven|\bcqrs\b|event\s*sourcing/i,
  ),
  t('concept-load-balancing', 'Load balancing', 'architecture', /load\s*balanc/i),
  t('concept-idempotency', 'Idempotency', 'backend', /idempoten/i),
  t(
    'concept-rest-api',
    'REST API design',
    'backend',
    /rest\s*api|api\s*design|http\s*(methods|status)/i,
  ),
  t('concept-grpc-graphql', 'gRPC / GraphQL', 'backend', /\bgrpc\b|graphql/i),
  t(
    'concept-auth',
    'AuthN/AuthZ, JWT, OAuth',
    'backend',
    /\bjwt\b|oauth|authentication\s*vs|authorization/i,
  ),
  t(
    'concept-websocket',
    'WebSockets / long polling / SSE',
    'backend',
    /websocket|long\s*polling|server[- ]sent/i,
  ),
  t('concept-cdn', 'CDN', 'architecture', /\bcdn\b/i),
  t(
    'concept-observability',
    'Observability / monitoring',
    'devops',
    /observability|prometheus|grafana|distributed\s*tracing|opentelemetry/i,
  ),
  t('concept-docker-k8s', 'Docker / Kubernetes', 'devops', /docker|kubernetes|\bk8s\b/i),
  t('concept-cicd', 'CI/CD', 'devops', /ci\s*\/?\s*cd|jenkins|github\s*actions/i),
  t('concept-cloud', 'Cloud (AWS/GCP/Azure)', 'devops', /\baws\b|\bgcp\b|azure|lambda|ec2/i),
  t(
    'concept-networking',
    'Networking (TCP/HTTP/DNS)',
    'cs',
    /\btcp\b|\budp\b|\bdns\b|osi\s*model|https?\s*handshake|tls/i,
  ),
  t(
    'concept-os',
    'OS (process/thread, memory, scheduling)',
    'cs',
    /process\s*vs\s*thread|virtual\s*memory|paging|context\s*switch|operating\s*system|\bos\s*concepts/i,
  ),
  t(
    'concept-big-o',
    'Complexity analysis',
    'cs',
    /time\s*complexity|space\s*complexity|big[- ]?o\b/i,
  ),

  // ── Java / concurrency
  t(
    'java-hashmap',
    'HashMap internals',
    'java',
    /hash\s*map\s*(internal|work|implementation)|how\s*hashmap/i,
  ),
  t('java-concurrenthashmap', 'ConcurrentHashMap', 'concurrency', /concurrent\s*hash\s*map/i),
  t(
    'java-gc-jvm',
    'JVM / garbage collection',
    'java',
    /garbage\s*collect|\bjvm\b|\bgc\b|heap\s*(vs|and)\s*stack|memory\s*leak/i,
  ),
  t(
    'java-streams',
    'Streams / lambdas / Java 8+',
    'java',
    /stream\s*api|java\s*8|lambda|functional\s*interface/i,
  ),
  t(
    'java-modern',
    'Modern Java (records, sealed, virtual threads)',
    'java',
    /virtual\s*thread|project\s*loom|\brecords?\b.*java|sealed\s*class|java\s*(17|21|25)/i,
  ),
  t(
    'java-collections',
    'Collections framework',
    'java',
    /collections?\s*framework|arraylist\s*vs|linked\s*list\s*vs|treemap|hashset/i,
  ),
  t(
    'java-oops',
    'OOP / SOLID',
    'lld',
    /\boops?\b|solid\s*principle|polymorphism|inheritance|abstraction|encapsulation/i,
  ),
  t(
    'java-design-patterns',
    'Design patterns',
    'lld',
    /design\s*pattern|singleton|factory\s*pattern|observer\s*pattern|strategy\s*pattern|builder\s*pattern|decorator/i,
  ),
  t(
    'java-exceptions',
    'Exceptions',
    'java',
    /checked\s*(vs|and)\s*unchecked|exception\s*handling/i,
  ),
  t('java-string', 'String immutability / string pool', 'java', /string\s*(pool|immutab)/i),
  t(
    'concurrency-threads',
    'Threads / synchronization / locks',
    'concurrency',
    /multi-?threading|synchroniz|deadlock|race\s*condition|mutex|semaphore|reentrant\s*lock|volatile/i,
  ),
  t(
    'concurrency-completable',
    'CompletableFuture / async',
    'concurrency',
    /completable\s*future|async\s*(programming|await)/i,
  ),
  t(
    'spring-core',
    'Spring / Spring Boot (DI, beans, AOP)',
    'backend',
    /spring\s*boot|\bspring\b.*\b(bean|di|ioc|aop)|dependency\s*injection|@transactional|@autowired/i,
  ),
  t('spring-security', 'Spring Security', 'backend', /spring\s*security/i),
  t(
    'jpa-hibernate',
    'JPA / Hibernate (N+1, lazy loading)',
    'backend',
    /hibernate|\bjpa\b|n\s*\+\s*1|lazy\s*loading/i,
  ),
  t(
    'testing',
    'Unit testing / mocking',
    'backend',
    /junit|mockito|unit\s*test|test\s*cases|\btdd\b/i,
  ),
  t('node-event-loop', 'Node.js event loop / async', 'concurrency', /event\s*loop|node\.?js/i),
  t('python-gil', 'Python GIL / asyncio', 'concurrency', /\bgil\b|asyncio/i),

  // ── DSA patterns
  t(
    'dsa-arrays-hashing',
    'Arrays & hashing',
    'dsa',
    /two\s*sum|subarray\s*sum|prefix\s*sum|hash\s*(map|set)\s*(problem|question)/i,
  ),
  t('dsa-two-pointers', 'Two pointers', 'dsa', /two[- ]pointer/i),
  t('dsa-sliding-window', 'Sliding window', 'dsa', /sliding\s*window|longest\s*substring/i),
  t(
    'dsa-binary-search',
    'Binary search',
    'dsa',
    /binary\s*search|search\s*in\s*rotated|koko|median\s*of\s*two\s*sorted/i,
  ),
  t(
    'dsa-stack',
    'Stack / monotonic stack',
    'dsa',
    /monotonic|next\s*greater|valid\s*parenthes|largest\s*rectangle|\bstack\b.*(problem|question)/i,
  ),
  t(
    'dsa-linked-list',
    'Linked lists',
    'dsa',
    /linked\s*list\s*(problem|question|cycle|reverse)|reverse\s*(a\s*)?linked\s*list|merge\s*k\s*sorted/i,
  ),
  t(
    'dsa-heap',
    'Heap / priority queue / top-k',
    'dsa',
    /\bheap\b|priority\s*queue|kth\s*(largest|smallest)|top\s*k\b/i,
  ),
  t(
    'dsa-trees',
    'Trees / BST',
    'dsa',
    /binary\s*tree|\bbst\b|lowest\s*common\s*ancestor|\blca\b|level\s*order|tree\s*traversal|serialize/i,
  ),
  t(
    'dsa-graphs',
    'Graphs (BFS/DFS)',
    'dsa',
    /\bgraph\b|\bbfs\b|\bdfs\b|number\s*of\s*islands|connected\s*component/i,
  ),
  t('dsa-topo-sort', 'Topological sort', 'dsa', /topolog|course\s*schedule/i),
  t(
    'dsa-shortest-path',
    'Shortest path (Dijkstra etc.)',
    'dsa',
    /dijkstra|shortest\s*path|bellman|floyd/i,
  ),
  t('dsa-union-find', 'Union-find', 'dsa', /union[- ]find|disjoint\s*set|\bdsu\b/i),
  t('dsa-trie', 'Trie', 'dsa', /\btrie\b|prefix\s*tree/i),
  t('dsa-intervals', 'Intervals', 'dsa', /interval|meeting\s*rooms?\s*(ii|2)\b/i),
  t(
    'dsa-backtracking',
    'Backtracking',
    'dsa',
    /backtrack|permutation|combination\s*sum|n[- ]queens|subsets/i,
  ),
  t('dsa-greedy', 'Greedy', 'dsa', /greedy|jump\s*game|gas\s*station/i),
  t(
    'dsa-dp',
    'Dynamic programming',
    'dsa',
    /dynamic\s*programming|\bdp\b|memoiz|knapsack|longest\s*common\s*subsequence|edit\s*distance|coin\s*change/i,
  ),
  t('dsa-bit', 'Bit manipulation', 'dsa', /bit\s*manipulation|\bxor\b/i),
  t('dsa-matrix', 'Matrix / grid', 'dsa', /\bmatrix\b|\bgrid\b|spiral/i),
  t('dsa-string', 'Strings', 'dsa', /anagram|palindrome|string\s*(manipulation|problem)/i),
  t(
    'dsa-design-ds',
    'Design a data structure',
    'dsa',
    /design\s*(a\s*)?(data\s*structure|hit\s*counter|min\s*stack|iterator|time\s*based)/i,
  ),

  // ── Behavioural & AI-era
  t(
    'behavioral',
    'Behavioural / leadership',
    'behavioral',
    /behaviou?ral|leadership\s*principle|\blp\s*round|bar\s*raiser|hiring\s*manager|\bhm\s*round|tell\s*me\s*about\s*a\s*time|conflict/i,
  ),
  t(
    'project-deep-dive',
    'Project deep dive',
    'behavioral',
    /project\s*(discussion|deep\s*dive|round)|past\s*project|resume\s*(discussion|deep)/i,
  ),
  t(
    'ai-assisted-coding',
    'AI-assisted coding round',
    'ai',
    /ai[- ]assisted|ai[- ]enabled|copilot\s*(allowed|round)|use\s*of\s*ai\s*(tools|allowed)|cursor\s*allowed/i,
  ),
  t(
    'ai-llm-concepts',
    'LLM concepts (embeddings, vector DB, prompting)',
    'ai',
    /embedding|vector\s*(db|database|store|search)|prompt\s*engineering|fine[- ]tun|\bllms?\b|agentic|\bagents?\b.*\b(ai|llm)/i,
  ),
  t(
    'debugging-round',
    'Debugging / code review round',
    'behavioral',
    /debugging\s*round|code\s*review\s*round|bug\s*(bash|fix)\s*round/i,
  ),
];

interface Post {
  title: string;
  slug: string;
  topicId: number;
  createdAt: string;
  company: string;
  tags: { slug: string }[];
}

const url = (p: Post) => `https://leetcode.com/discuss/post/${p.topicId}/${p.slug}/`;

async function main() {
  const index: Record<string, Post> = JSON.parse(
    await readFile(join(cacheDir, 'index.json'), 'utf8'),
  );
  const posts = Object.values(index);
  const counts = new Map<
    string,
    { posts: number; companies: Map<string, number>; byYear: Record<string, number> }
  >();
  const evidence = new Map<
    string,
    {
      company: string;
      topic: string;
      label: string;
      category: Category;
      reports: { year: number; month: number; title: string; source: string }[];
    }
  >();
  let withContent = 0;

  for (const p of posts) {
    const body = await readFile(join(cacheDir, `${p.topicId}.md`), 'utf8').catch(() => '');
    if (!body) continue;
    withContent++;
    const text = `${p.title}\n${body}`.replace(/\\n/g, '\n');
    const d = new Date(p.createdAt);
    const year = d.getUTCFullYear();
    for (const topic of TOPICS) {
      if (!topic.re.test(text)) continue;
      const c = counts.get(topic.key) ?? {
        posts: 0,
        companies: new Map<string, number>(),
        byYear: {} as Record<string, number>,
      };
      c.posts++;
      c.companies.set(p.company, (c.companies.get(p.company) ?? 0) + 1);
      c.byYear[String(year)] = (c.byYear[String(year)] ?? 0) + 1;
      counts.set(topic.key, c);
      const ek = `${p.company}|${topic.key}`;
      const e = evidence.get(ek) ?? {
        company: p.company,
        topic: topic.key,
        label: topic.label,
        category: topic.category,
        reports: [],
      };
      e.reports.push({ year, month: d.getUTCMonth() + 1, title: p.title.trim(), source: url(p) });
      evidence.set(ek, e);
    }
  }

  const matrix = TOPICS.map((tp) => {
    const c = counts.get(tp.key);
    return {
      key: tp.key,
      label: tp.label,
      category: tp.category,
      posts: c?.posts ?? 0,
      share: c ? +((c.posts / withContent) * 100).toFixed(2) : 0,
      companies: c ? c.companies.size : 0,
      topCompanies: c
        ? [...c.companies.entries()]
            .sort((a, b) => b[1] - a[1])
            .slice(0, 12)
            .map(([n, k]) => `${n} (${k})`)
        : [],
      byYear: c?.byYear ?? {},
    };
  }).sort((a, b) => b.posts - a.posts);

  const companyQuestions = [...evidence.values()]
    .map((e) => ({ ...e, reports: e.reports.sort((a, b) => b.year - a.year || b.month - a.month) }))
    .sort((a, b) => a.company.localeCompare(b.company) || b.reports.length - a.reports.length);

  const meta = {
    generatedAt: new Date().toISOString().slice(0, 10),
    source:
      'LeetCode Discuss public interview posts (company-tagged), via scripts/research/leetcode-discuss.ts',
    postsAnalysed: withContent,
    companies: new Set(posts.map((p) => p.company)).size,
    window: `${posts
      .map((p) => p.createdAt)
      .sort()[0]
      ?.slice(0, 10)} → ${posts
      .map((p) => p.createdAt)
      .sort()
      .at(-1)
      ?.slice(0, 10)}`,
    method:
      'Conservative phrase matching on title + body. A post counts once per topic. Counts are mentions in candidate reports, not official question banks.',
  };
  await writeFile(
    join(root, 'research/topic-matrix.json'),
    JSON.stringify({ meta, topics: matrix }, null, 2),
  );
  await writeFile(
    join(root, 'research/company-questions.json'),
    JSON.stringify({ meta, entries: companyQuestions }, null, 2),
  );
  console.log(meta);
  console.table(
    matrix
      .slice(0, 60)
      .map((m) => ({ key: m.key, posts: m.posts, share: m.share, companies: m.companies })),
  );
}

await main();

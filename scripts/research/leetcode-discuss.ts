/**
 * Collects public interview-experience posts from LeetCode Discuss (public GraphQL endpoint),
 * caches them locally (.cache/research, git-ignored) and writes nothing else.
 * Analysis happens in analyze.ts so it can be re-run without re-fetching.
 *
 *   npx tsx scripts/research/leetcode-discuss.ts [--since 2024-07-01] [--per-company 400]
 *
 * Be polite: requests are sequential with a delay. Only post metadata + text are cached.
 */
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const ENDPOINT = 'https://leetcode.com/graphql/';
const root = new URL('../../', import.meta.url).pathname;
const cacheDir = join(root, '.cache/research/leetcode');
const arg = (name: string, fallback: string) => {
  const i = process.argv.indexOf(name);
  return i > 0 ? process.argv[i + 1]! : fallback;
};
const SINCE = new Date(arg('--since', '2024-07-01'));
const PER_COMPANY = Number(arg('--per-company', '400'));
const DELAY_MS = 350;

/** LeetCode tag slug → display name. Tags that return nothing are skipped. */
export const COMPANIES: Record<string, string> = {
  amazon: 'Amazon',
  google: 'Google',
  microsoft: 'Microsoft',
  meta: 'Meta',
  facebook: 'Meta',
  apple: 'Apple',
  uber: 'Uber',
  flipkart: 'Flipkart',
  walmart: 'Walmart',
  'walmart-labs': 'Walmart',
  atlassian: 'Atlassian',
  salesforce: 'Salesforce',
  oracle: 'Oracle',
  adobe: 'Adobe',
  'goldman-sachs': 'Goldman Sachs',
  jpmorgan: 'JPMorgan Chase',
  'morgan-stanley': 'Morgan Stanley',
  intuit: 'Intuit',
  paypal: 'PayPal',
  visa: 'Visa',
  linkedin: 'LinkedIn',
  netflix: 'Netflix',
  airbnb: 'Airbnb',
  stripe: 'Stripe',
  doordash: 'DoorDash',
  swiggy: 'Swiggy',
  zomato: 'Zomato',
  phonepe: 'PhonePe',
  razorpay: 'Razorpay',
  cred: 'CRED',
  paytm: 'Paytm',
  meesho: 'Meesho',
  myntra: 'Myntra',
  'de-shaw': 'D. E. Shaw',
  bloomberg: 'Bloomberg',
  servicenow: 'ServiceNow',
  cisco: 'Cisco',
  nvidia: 'NVIDIA',
  rippling: 'Rippling',
  coinbase: 'Coinbase',
  booking: 'Booking.com',
  expedia: 'Expedia',
  sprinklr: 'Sprinklr',
  shopify: 'Shopify',
  databricks: 'Databricks',
  snowflake: 'Snowflake',
  tiktok: 'TikTok',
  bytedance: 'ByteDance',
  openai: 'OpenAI',
  zeta: 'Zeta',
  thoughtworks: 'Thoughtworks',
  'american-express': 'American Express',
  'media-net': 'Media.net',
  arcesium: 'Arcesium',
  nutanix: 'Nutanix',
  twilio: 'Twilio',
  zscaler: 'Zscaler',
  confluent: 'Confluent',
  roblox: 'Roblox',
  pinterest: 'Pinterest',
  snap: 'Snap',
  lyft: 'Lyft',
  robinhood: 'Robinhood',
  block: 'Block',
  docusign: 'DocuSign',
  zoho: 'Zoho',
  freshworks: 'Freshworks',
  groww: 'Groww',
  zepto: 'Zepto',
  ola: 'Ola',
  dream11: 'Dream11',
  juspay: 'Juspay',
};

interface ListNode {
  title: string;
  slug: string;
  topicId: number;
  createdAt: string;
  tags: { slug: string }[];
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

async function gql<T>(query: string, variables: object, attempt = 0): Promise<T> {
  const res = await fetch(ENDPOINT, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      referer: 'https://leetcode.com/discuss/',
      'user-agent': 'Mozilla/5.0 (research script)',
    },
    body: JSON.stringify({ query, variables }),
  });
  if (res.status === 429 || res.status >= 500) {
    if (attempt > 5) throw new Error(`HTTP ${res.status}`);
    await sleep(2000 * 2 ** attempt);
    return gql(query, variables, attempt + 1);
  }
  const json = (await res.json()) as { data: T; errors?: unknown };
  if (json.errors) throw new Error(JSON.stringify(json.errors).slice(0, 300));
  return json.data;
}

const LIST = `query l($orderBy: ArticleOrderByEnum, $keywords: [String]!, $tagSlugs: [String!], $skip: Int, $first: Int) {
  ugcArticleDiscussionArticles(orderBy: $orderBy, keywords: $keywords, tagSlugs: $tagSlugs, skip: $skip, first: $first) {
    totalNum edges { node { title slug topicId createdAt tags { slug } } } } }`;
const ARTICLE = `query a($topicId: ID) { ugcArticleDiscussionArticle(topicId: $topicId) { content } }`;

async function main() {
  await mkdir(cacheDir, { recursive: true });
  const indexPath = join(cacheDir, 'index.json');
  const index: Record<string, ListNode & { company: string }> = JSON.parse(
    await readFile(indexPath, 'utf8').catch(() => '{}'),
  );

  for (const [tag, company] of Object.entries(COMPANIES)) {
    let skip = 0;
    let added = 0;
    while (skip < PER_COMPANY) {
      const data = await gql<{ ugcArticleDiscussionArticles: { edges: { node: ListNode }[] } }>(
        LIST,
        {
          orderBy: 'MOST_RECENT',
          keywords: [''],
          tagSlugs: [tag],
          skip,
          first: 50,
        },
      );
      await sleep(DELAY_MS);
      const nodes = data.ugcArticleDiscussionArticles.edges.map((e) => e.node);
      if (!nodes.length) break;
      let old = false;
      for (const n of nodes) {
        if (new Date(n.createdAt) < SINCE) {
          old = true;
          continue;
        }
        // An unknown tag returns unfiltered results, so verify the tag is really on the post.
        const tags = n.tags.map((t) => t.slug);
        if (!tags.includes(tag)) continue;
        if (
          !tags.includes('interview') &&
          !/interview|round|experience|loop|onsite|\boa\b/i.test(n.title)
        )
          continue;
        if (!index[n.topicId]) {
          index[n.topicId] = { ...n, company };
          added++;
        }
      }
      if (old || !nodes.some((n) => n.tags.some((t) => t.slug === tag))) break;
      skip += 50;
    }
    console.log(`${company.padEnd(18)} +${added}`);
    await writeFile(indexPath, JSON.stringify(index));
  }

  const ids = Object.keys(index);
  let fetched = 0;
  for (const id of ids) {
    const file = join(cacheDir, `${id}.md`);
    if (
      await readFile(file).then(
        () => true,
        () => false,
      )
    )
      continue;
    try {
      const data = await gql<{ ugcArticleDiscussionArticle: { content: string } | null }>(ARTICLE, {
        topicId: id,
      });
      await writeFile(file, data.ugcArticleDiscussionArticle?.content ?? '');
      fetched++;
      if (fetched % 100 === 0) console.log(`content ${fetched}/${ids.length}`);
    } catch (e) {
      console.warn(`skip ${id}: ${(e as Error).message}`);
    }
    await sleep(DELAY_MS);
  }
  console.log(`Done. ${ids.length} posts indexed, ${fetched} newly fetched.`);
}

await main();

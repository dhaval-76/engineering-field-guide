import { getCollection } from 'astro:content';
import { getFlatTopics, slugifyCompany } from './nav';
import { topicUrl } from './url';

export interface CompanyItem {
  kind: 'topic' | 'question' | 'follow-up';
  text: string;
  topicId: string;
  topicTitle: string;
  url: string;
  year?: number | undefined;
  round?: string | undefined;
  source: string;
}

export interface Company {
  name: string;
  slug: string;
  items: CompanyItem[];
}

/** Every askedAt tag across topics and Q&A, grouped by company. */
export async function getCompanyIndex(): Promise<Company[]> {
  const topics = await getFlatTopics();
  const titles = new Map(topics.map((t) => [t.id, t.data.title]));
  const map = new Map<string, Company>();
  const add = (company: string, item: CompanyItem) => {
    const slug = slugifyCompany(company);
    const c = map.get(slug) ?? { name: company, slug, items: [] };
    c.items.push(item);
    map.set(slug, c);
  };
  for (const t of topics) {
    for (const a of t.data.askedAt)
      add(a.company, {
        kind: 'topic',
        text: t.data.title,
        topicId: t.id,
        topicTitle: t.data.title,
        url: topicUrl(t.id),
        year: a.year,
        round: a.round,
        source: a.source,
      });
  }
  for (const qa of await getCollection('qa')) {
    const title = titles.get(qa.id);
    if (!title) continue;
    for (const q of qa.data.questions)
      for (const a of q.askedAt)
        add(a.company, {
          kind: 'question',
          text: q.q,
          topicId: qa.id,
          topicTitle: title,
          url: `${topicUrl(qa.id)}#interview-questions`,
          year: a.year,
          round: a.round,
          source: a.source,
        });
    for (const q of qa.data.followUps)
      for (const a of q.askedAt)
        add(a.company, {
          kind: 'follow-up',
          text: q.q,
          topicId: qa.id,
          topicTitle: title,
          url: `${topicUrl(qa.id)}#follow-ups`,
          year: a.year,
          round: a.round,
          source: a.source,
        });
  }
  return [...map.values()].sort((a, b) => a.name.localeCompare(b.name));
}

import { getCollection, type CollectionEntry } from 'astro:content';
import { getFlatTopics, slugifyCompany } from './nav';
import { topicUrl } from './url';

export interface CompanyItem {
  kind: 'topic' | 'question' | 'follow-up' | 'reported';
  text: string;
  topicId: string;
  topicTitle: string;
  url: string;
  year?: number | undefined;
  round?: string | undefined;
  source: string;
  /** For research-derived items: number of public reports mentioning the topic. */
  reports?: number | undefined;
  /** Markdown answer, for questions tagged on a topic page. */
  answer?: string | undefined;
}

export const SECTIONS = [
  { id: 'dsa', label: 'DSA' },
  { id: 'lld', label: 'Low-level design' },
  { id: 'hld', label: 'System design' },
  { id: 'language', label: 'Java and concurrency' },
  { id: 'backend', label: 'Backend and databases' },
  { id: 'fundamentals', label: 'CS fundamentals' },
  { id: 'frontend', label: 'Frontend' },
  { id: 'ai', label: 'AI and code repository' },
  { id: 'behavioral', label: 'Behavioural' },
] as const;
export type SectionId = (typeof SECTIONS)[number]['id'];

const TRACK_SECTION: Record<string, SectionId> = {
  dsa: 'dsa',
  'low-level-design': 'lld',
  'oop-patterns': 'lld',
  'system-design': 'hld',
  'distributed-systems': 'hld',
  'java-runtime': 'language',
  concurrency: 'language',
  backend: 'backend',
  databases: 'backend',
  devops: 'backend',
  'cs-foundations': 'fundamentals',
  'ai-engineering': 'ai',
  interviews: 'behavioral',
};
export const sectionOfTopic = (topicId: string): SectionId =>
  TRACK_SECTION[topicId.split('/')[0]!] ?? 'backend';

export type CompanyQuestion = CollectionEntry<'companyQuestions'>['data']['questions'][number];

export interface Company {
  name: string;
  slug: string;
  items: CompanyItem[];
  /** Curated questions with solutions, from src/content/company-questions/. */
  questions: CompanyQuestion[];
  overview: string[];
}

/** Every askedAt tag across topics and Q&A, grouped by company. */
export async function getCompanyIndex(): Promise<Company[]> {
  const topics = await getFlatTopics();
  const titles = new Map(topics.map((t) => [t.id, t.data.title]));
  const map = new Map<string, Company>();
  const add = (company: string, item: CompanyItem) => {
    const slug = slugifyCompany(company);
    const c = map.get(slug) ?? { name: company, slug, items: [], questions: [], overview: [] };
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
          answer: q.answer,
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
          answer: q.approach,
        });
  }
  for (const r of await getCollection('reports')) {
    const title = titles.get(r.id);
    if (!title) continue;
    for (const c of r.data.companies)
      add(c.company, {
        kind: 'reported',
        text: title,
        topicId: r.id,
        topicTitle: title,
        url: topicUrl(r.id),
        year: c.latestYear,
        source: c.sources[0]!.url,
        reports: c.reports,
      });
  }
  for (const entry of await getCollection('companyQuestions')) {
    const slug = slugifyCompany(entry.data.company);
    const c = map.get(slug) ?? {
      name: entry.data.company,
      slug,
      items: [],
      questions: [],
      overview: [],
    };
    c.questions = entry.data.questions;
    c.overview = entry.data.overview;
    map.set(slug, c);
  }
  return [...map.values()].sort((a, b) => a.name.localeCompare(b.name));
}

export const getTrackIdOf = (topicId: string) => topicId.split('/')[0]!;

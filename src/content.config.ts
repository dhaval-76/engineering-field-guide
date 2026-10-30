import { defineCollection, reference } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';

/** Folder path of a topic entry, e.g. `low-level-design/lru-cache`. */
const topicIdFromPath = (entry: string) => entry.replace(/\/(index\.mdx|qa\.ya?ml)$/, '');

const markdown = z.string().min(1);

/** Where a question/topic was reported. `source` must point at the public report. */
const askedAt = z
  .array(
    z.object({
      company: z.string().min(1),
      year: z.number().int().min(2015).max(2100).optional(),
      round: z.string().optional(),
      source: z.url(),
    }),
  )
  .default([]);

const tracks = defineCollection({
  loader: glob({ pattern: '*.yaml', base: './src/content/tracks' }),
  schema: z.object({
    title: z.string(),
    short: z.string().describe('Short label for tight spaces'),
    blurb: z.string(),
    order: z.number().int(),
    icon: z.enum([
      'foundations',
      'dsa',
      'runtime',
      'concurrency',
      'patterns',
      'lld',
      'backend',
      'database',
      'system',
      'architecture',
      'devops',
      'career',
      'ai',
    ]),
  }),
});

const topics = defineCollection({
  loader: glob({
    pattern: '**/index.mdx',
    base: './src/content/topics',
    generateId: ({ entry }) => topicIdFromPath(entry),
  }),
  schema: z.object({
    title: z.string(),
    summary: z.string().max(240),
    track: reference('tracks'),
    order: z.number(),
    tags: z.array(z.string()).default([]),
    aliases: z.array(z.string()).default([]).describe('Other names people search for'),
    prerequisites: z.array(reference('topics')).default([]),
    related: z.array(reference('topics')).default([]),
    quickRef: z.object({
      points: z.array(z.string()).min(1),
      complexity: z.string().optional(),
      useWhen: z.string().optional(),
    }),
    askedAt,
    lastReviewed: z.coerce.date(),
    draft: z.boolean().default(false),
  }),
});

const qa = defineCollection({
  loader: glob({
    pattern: '**/qa.yaml',
    base: './src/content/topics',
    generateId: ({ entry }) => topicIdFromPath(entry),
  }),
  schema: z.object({
    questions: z
      .array(z.object({ q: z.string(), think: markdown.optional(), answer: markdown, askedAt }))
      .default([]),
    followUps: z
      .array(z.object({ q: z.string(), intuition: markdown, approach: markdown, askedAt }))
      .default([]),
    practice: z
      .array(
        z.object({
          task: markdown,
          hint: markdown.optional(),
          solution: markdown,
          solutionCode: z.string().optional().describe('Snippet id rendered as code tabs'),
        }),
      )
      .default([]),
    quiz: z
      .array(
        z
          .object({
            q: z.string(),
            options: z.array(z.string()).min(2),
            correct: z.number().int().min(0),
            why: markdown,
          })
          .refine((x) => x.correct < x.options.length, 'correct index out of range'),
      )
      .default([]),
  }),
});

const glossary = defineCollection({
  loader: glob({ pattern: '*.yaml', base: './src/content/glossary' }),
  schema: z.object({
    term: z.string(),
    short: z.string().max(220),
    topic: reference('topics').optional(),
  }),
});

const cell = z.union([
  z.string(),
  z.number(),
  z.object({
    v: z.union([z.string(), z.number()]),
    tone: z.enum(['accent', 'ok', 'warn', 'bad', 'muted', 'info', 'plum']).optional(),
    tag: z.string().optional().describe('Small label under the cell, e.g. a pointer name'),
  }),
]);

const walkthroughs = defineCollection({
  loader: glob({
    pattern: '**/walkthroughs/*.yaml',
    base: './src/content/topics',
    generateId: ({ entry }) =>
      entry
        .split('/')
        .pop()!
        .replace(/\.ya?ml$/, ''),
  }),
  schema: z.object({
    title: z.string(),
    frames: z
      .array(
        z.object({
          caption: markdown,
          panels: z.array(
            z.object({
              kind: z.enum(['array', 'list', 'rows', 'kv', 'stack', 'queue', 'note']),
              label: z.string().optional(),
              items: z.array(cell).default([]),
              /** for kv panels: pairs */
              pairs: z.array(z.tuple([cell, cell])).default([]),
              text: z.string().optional(),
            }),
          ),
        }),
      )
      .min(1),
  }),
});

/** Generated from research (scripts/research/apply-reports.ts): companies where a topic was reported. */
const reports = defineCollection({
  loader: glob({
    pattern: '**/*.yaml',
    base: './src/content/reports',
    generateId: ({ entry }) => entry.replace(/\.ya?ml$/, ''),
  }),
  schema: z.object({
    generatedAt: z.coerce.date(),
    source: z.string(),
    window: z.string(),
    companies: z.array(
      z.object({
        company: z.string(),
        reports: z.number().int().positive(),
        latestYear: z.number().int(),
        sources: z.array(z.object({ url: z.url(), title: z.string(), year: z.number().int() })).min(1),
      }),
    ),
  }),
});

export const collections = { tracks, topics, qa, glossary, walkthroughs, reports };

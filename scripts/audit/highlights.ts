/**
 * End-to-end test of highlights and the notes page, using real selections on built topic pages:
 * drawing, finding text again, notes, overlaps, areas that must stay untouched, going to a
 * highlight, the notes page, backups and hostile imported text.
 *   npm run build && npm run preview   (in another terminal)
 *   npm run test:highlights
 */
import { chromium, type Browser, type BrowserContextOptions, type Page } from 'playwright';
import { BASE } from './pages';

const failures: string[] = [];
const expect = (ok: boolean, what: string) => {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${what}`);
  if (!ok) failures.push(what);
};
const TOPIC = 'dsa/sliding-window';
const now = Date.now();
type Stored = Record<string, { topic: string; quote: string; note?: string; at: number }>;

async function open(
  browser: Browser,
  path = `${TOPIC}/`,
  options: BrowserContextOptions = {},
  seed?: Stored,
) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, ...options });
  await ctx.addInitScript('globalThis.__name = (f) => f');
  await ctx.route('**/sw.js', (r) => r.abort());
  if (seed)
    await ctx.addInitScript(
      `localStorage.setItem('efg:highlights', ${JSON.stringify(JSON.stringify(seed))})`,
    );
  const page = await ctx.newPage();
  page.on('pageerror', (e) => failures.push(`page error: ${e.message}`));
  page.on('console', (m) => {
    if (m.type() === 'error' && /hydrat/i.test(m.text())) failures.push(`hydration: ${m.text()}`);
  });
  await page.goto(BASE + path);
  return page;
}
const stored = (page: Page) =>
  page.evaluate(() =>
    JSON.parse(localStorage.getItem('efg:highlights') ?? '{}'),
  ) as Promise<Stored>;
const one = async (page: Page) => Object.values(await stored(page))[0];
const marks = (page: Page) => page.locator('mark.hl');
const bar = (page: Page) => page.locator('.hl-bar:not([hidden])');

/** Selects the `nth` occurrence of `text` in the first text node, or across nodes, that holds it. */
const select = (page: Page, text: string, nth = 0, root = '[data-highlight-root]') =>
  page.evaluate(
    ([text, nth, root]) => {
      const el = [...document.querySelectorAll(root as string)];
      const nodes: Text[] = [];
      for (const r of el) {
        const w = document.createTreeWalker(r, NodeFilter.SHOW_TEXT);
        while (w.nextNode())
          if (!(w.currentNode.parentElement as Element).closest('astro-island, svg, [popover]'))
            nodes.push(w.currentNode as Text);
      }
      const full = nodes.map((n) => n.data).join('');
      let at = -1;
      for (let i = 0; i <= (nth as number); i++) at = full.indexOf(text as string, at + 1);
      if (at < 0) throw new Error(`not on page: ${text}`);
      const end = at + (text as string).length;
      let pos = 0;
      const range = document.createRange();
      for (const n of nodes) {
        const next = pos + n.length;
        if (at >= pos && at < next) range.setStart(n, at - pos);
        if (end > pos && end <= next) range.setEnd(n, end - pos);
        pos = next;
      }
      const s = getSelection()!;
      s.removeAllRanges();
      s.addRange(range);
    },
    [text, nth, root] as const,
  );
const pageText = (page: Page) =>
  page.evaluate(() =>
    [...document.querySelectorAll('[data-highlight-root]')].map((r) => r.textContent).join(''),
  );
const clickBar = async (page: Page, label: string) => {
  await bar(page).waitFor();
  await bar(page).getByRole('button', { name: label }).click();
};
/** A phrase that really occurs in the page's main text. */
const phrases = (page: Page) =>
  page.evaluate(() => {
    const longest = [...document.querySelectorAll('[data-highlight-root] p')]
      .map((p) => p.firstChild)
      .find((n) => n?.nodeType === 3 && (n as Text).length > 70) as Text;
    const a = longest.data;
    const mixed = [...document.querySelectorAll('[data-highlight-root] p')].find(
      (p) =>
        p.querySelector('strong, code') &&
        p.firstChild?.nodeType === 3 &&
        (p.firstChild as Text).length > 12 &&
        p.querySelector('strong, code')!.nextSibling?.nodeType === 3,
    )!;
    const inner = mixed.querySelector('strong, code')!;
    return {
      plain: a.slice(10, 60).trim(),
      across:
        (mixed.firstChild as Text).data.slice(-8) +
        inner.textContent +
        (inner.nextSibling as Text).data.slice(0, 6),
    };
  });

const browser = await chromium.launch();
try {
  {
    const page = await open(browser);
    const before = await pageText(page);
    const { plain, across } = await phrases(page);

    await select(page, plain);
    await clickBar(page, 'Highlight');
    expect((await marks(page).count()) === 1, 'selecting text and pressing Highlight draws it');
    expect((await one(page))?.quote === plain, 'the exact text is stored');
    expect((await pageText(page)) === before, 'highlighting does not change the text of the page');
    await page.reload();
    expect(
      (await marks(page).count()) === 1 && (await marks(page).first().textContent()) === plain,
      'a highlight is back after reload',
    );

    await marks(page).first().click();
    await clickBar(page, 'Add note');
    await page.fill('.hl-note textarea', 'why this matters');
    await page.click('.hl-note [data-save]');
    expect((await one(page))?.note === 'why this matters', 'a note can be added to a highlight');
    expect(
      (await marks(page).first().getAttribute('title')) === 'why this matters',
      'the note shows on the highlight',
    );

    await select(page, plain.slice(5, 20));
    await clickBar(page, 'Highlight');
    expect(Object.keys(await stored(page)).length === 1, 'an overlapping selection is refused');
    await page.evaluate(() => getSelection()!.removeAllRanges());

    await marks(page).first().click();
    await clickBar(page, 'Remove');
    expect(
      (await marks(page).count()) === 0 && Object.keys(await stored(page)).length === 0,
      'Remove deletes it',
    );
    expect((await pageText(page)) === before, 'removing puts the page text back exactly');

    await select(page, across);
    await clickBar(page, 'Highlight');
    const n = await marks(page).count();
    const joined = (await marks(page).allTextContents()).join('');
    expect(n >= 2 && joined === across, `a selection across formatting is drawn in ${n} parts`);
    expect((await pageText(page)) === before, 'formatted text is unchanged');
    await page.reload();
    expect(
      (await marks(page).allTextContents()).join('') === across,
      'a selection across formatting survives reload',
    );
    await page.context().close();
  }

  // The same words twice: the context picks the right one
  {
    const page = await open(browser);
    const twice = await page.evaluate(() => {
      let t = '';
      for (const r of document.querySelectorAll('[data-highlight-root]')) {
        const w = document.createTreeWalker(r, NodeFilter.SHOW_TEXT);
        while (w.nextNode())
          if (!w.currentNode.parentElement!.closest('astro-island, svg, [popover], button'))
            t += (w.currentNode as Text).data;
      }
      return ['sliding window', 'window', 'the'].find((w) => t.split(w).length > 4) ?? '';
    });
    await select(page, twice, 2);
    await clickBar(page, 'Highlight');
    const where = () =>
      page.evaluate(() => {
        const m = document.querySelector('mark.hl')!;
        const r = document.createRange();
        r.setStart(document.querySelector('[data-highlight-root]')!, 0);
        r.setEndBefore(m);
        return r.toString().length;
      });
    const first = await where();
    await page.reload();
    expect(
      (await where()) === first && first > 0,
      'repeated text returns to the same place, not the first match',
    );
    await page.context().close();
  }

  // Shortcuts
  {
    const page = await open(browser);
    const { plain } = await phrases(page);
    await page.keyboard.press('h');
    await page.waitForFunction(() =>
      document.querySelector('.key-toast')?.textContent?.includes('Select some text'),
    );
    expect(true, 'h with nothing selected explains itself');
    await select(page, plain);
    await page.keyboard.press('h');
    expect((await marks(page).count()) === 1, 'h highlights the selection');
    await select(page, plain.slice(0, 0) + (await phrases(page)).across);
    await page.keyboard.press('n');
    await page.waitForSelector('.hl-note[open]');
    await page.keyboard.type('hnbm typed here');
    expect(
      (await page.inputValue('.hl-note textarea')) === 'hnbm typed here',
      'typing in the note box triggers no shortcuts',
    );
    await page.click('.hl-note [data-cancel]');
    expect(Object.keys(await stored(page)).length === 1, 'cancelling a new note creates nothing');
    await page.context().close();
  }

  // Must stay untouched
  {
    const page = await open(browser);
    await page.locator('[data-note] textarea').fill('some note text here');
    await page.locator('[data-note] textarea').selectText();
    await page.waitForTimeout(350);
    expect((await bar(page).count()) === 0, 'no bar for text in the notes box');
    await page.evaluate(() => {
      const h = document.querySelector('h1')!;
      getSelection()!.selectAllChildren(h);
    });
    await page.waitForTimeout(350);
    expect((await bar(page).count()) === 0, 'no bar outside the content');
    await page
      .locator('astro-island .wt-note, astro-island .wt-cell')
      .first()
      .scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
    await page.evaluate(() => {
      const c = document.querySelector('astro-island .wt-cell, astro-island .wt-note')!;
      getSelection()!.selectAllChildren(c);
    });
    await page.waitForTimeout(350);
    expect((await bar(page).count()) === 0, 'no bar inside a walkthrough');
    await page.context().close();
  }

  // Code, closed answers, going to a highlight
  {
    const page = await open(browser);
    const code = await page.evaluate(() => {
      const line = document.querySelector(
        '[data-highlight-root] [role=tabpanel]:not([hidden]) .line:nth-child(3)',
      )!;
      return line.textContent!.trim().slice(0, 18);
    });
    const before = await page.evaluate(
      () => document.querySelector('[role=tabpanel]:not([hidden]) code')!.textContent,
    );
    await select(page, code);
    await clickBar(page, 'Highlight');
    expect((await marks(page).allTextContents()).join('') === code, 'code can be highlighted');
    expect(
      (await page.evaluate(
        () => document.querySelector('[role=tabpanel]:not([hidden]) code')!.textContent,
      )) === before,
      'copying code would still give the original text',
    );

    await page.evaluate(() => {
      document.querySelector<HTMLDetailsElement>('.interview details.qa')!.open = true;
    });
    const answer = await page.evaluate(() =>
      document
        .querySelector('.interview details.qa .qa-body p:not(.tag)')!
        .textContent!.slice(0, 30),
    );
    await select(page, answer, 0, '.interview');
    await clickBar(page, 'Highlight');
    const id = Object.entries(await stored(page)).find(([, h]) => h.quote === answer)![0];
    await page.goto(`${BASE}shortcuts/`);
    await page.goto(`${BASE}${TOPIC}/#hl-${id}`);
    await page.waitForFunction(() => document.querySelector('mark.hl-flash'));
    await page.waitForTimeout(1200); // the page scrolls smoothly
    expect(
      await page.evaluate(() => document.querySelector('mark.hl-flash')!.closest('details')!.open),
      'going to a highlight opens the closed answer',
    );
    expect(
      await page.evaluate(() => {
        const r = document.querySelector('mark.hl-flash')!.getBoundingClientRect();
        return r.top >= 0 && r.bottom <= innerHeight;
      }),
      'going to a highlight scrolls to it',
    );
    await page.context().close();
  }

  // Text that no longer exists, and hostile imported text
  {
    const evil = '<img src=x onerror="window.__owned=1"><script>window.__owned=1</script>';
    const seed: Stored = {
      lost0001: {
        topic: TOPIC,
        quote: 'this sentence is not on the page anywhere',
        note: evil,
        at: now,
      } as Stored[string],
      evil0002: { topic: TOPIC, quote: 'window', note: evil, at: now - 5 } as Stored[string],
      gone0003: { topic: 'dsa/removed-topic', quote: evil, at: now - 9 } as Stored[string],
    };
    for (const h of Object.values(seed)) Object.assign(h, { prefix: '', suffix: '' });
    const page = await open(browser, `${TOPIC}/`, {}, seed);
    await page.waitForSelector('[data-hl-lost]:not([hidden])', { state: 'attached' });
    expect(
      (await page.innerText('[data-hl-lost]')).includes('1 of your highlights'),
      'a highlight whose text is gone is reported, not lost',
    );
    await marks(page).first().click();
    await clickBar(page, 'Edit note');
    expect(
      (await page.inputValue('.hl-note textarea')) === evil,
      'markup in a note stays plain text in the editor',
    );
    await page.click('.hl-note [data-cancel]');
    await page.goto(`${BASE}notes/`);
    await page.waitForSelector('.nt-topic');
    expect(
      (await page.locator('.nt-item').count()) === 3,
      'the notes page lists every highlight, even ones that can no longer be placed',
    );
    expect(
      (await page.innerText('.nt')).includes('no longer on the site'),
      'a removed topic is labelled',
    );
    expect(
      (await page.evaluate(() => (window as unknown as { __owned?: number }).__owned)) ===
        undefined,
      'markup in stored text never runs',
    );
    await page.context().close();
  }

  // Notes page
  {
    const seed: Stored = {
      aaaa0001: {
        topic: TOPIC,
        quote: 'window',
        note: 'first thought',
        at: now - 1000,
        prefix: '',
        suffix: '',
      } as Stored[string],
      bbbb0002: {
        topic: 'dsa/two-pointers',
        quote: 'pointers',
        at: now - 2000,
        prefix: '',
        suffix: '',
      } as Stored[string],
    };
    const page = await open(
      browser,
      'notes/',
      { viewport: { width: 360, height: 780 }, hasTouch: true, isMobile: true },
      seed,
    );
    await page.evaluate(
      (t) =>
        localStorage.setItem(
          'efg:notes',
          JSON.stringify({ [t]: { text: 'topic level note', at: Date.now() } }),
        ),
      TOPIC,
    );
    await page.reload();
    await page.waitForSelector('.nt-topic');
    expect((await page.locator('.nt-topic').count()) === 2, 'notes are grouped by topic');
    expect(
      await page
        .locator('.nt-topic')
        .first()
        .innerText()
        .then((t) => t.includes('topic level note')),
      'topic notes appear with highlights',
    );
    await page.fill('.nt-filter', 'first thought');
    expect((await page.locator('.nt-item').count()) === 1, 'the filter narrows the list');
    await page.fill('.nt-filter', '');
    await page.getByRole('button', { name: 'Edit note' }).first().click();
    await page.fill('.nt-edit textarea', 'second thought');
    await page.locator('.nt-edit').getByRole('button', { name: 'Save' }).click();
    expect((await stored(page)).aaaa0001!.note === 'second thought', 'a note can be edited here');
    await page.getByRole('button', { name: 'Remove' }).last().click();
    expect(!(await stored(page)).bbbb0002, 'a highlight can be removed here');
    expect(
      !(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)),
      'no sideways scroll at phone width',
    );
    await page.context().close();
  }

  // Touch: the bar docks clear of the browser's own menu
  {
    const page = await open(browser, `${TOPIC}/`, {
      viewport: { width: 360, height: 780 },
      hasTouch: true,
      isMobile: true,
    });
    const { plain } = await phrases(page);
    await select(page, plain);
    await bar(page).waitFor();
    const box = (await bar(page).boundingBox())!;
    expect(
      box.y + box.height > 700 && box.x >= 0 && box.x + box.width <= 360,
      'on a phone the bar docks at the bottom, on screen',
    );
    await bar(page).getByRole('button', { name: 'Highlight' }).tap();
    expect((await marks(page).count()) === 1, 'tapping Highlight works');
    expect(
      !(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)),
      'no sideways scroll on the topic',
    );
    await page.context().close();
  }

  // A real mouse drag, not a scripted selection
  {
    const page = await open(browser);
    const p = page.locator('[data-highlight-root] > p').first();
    await p.scrollIntoViewIfNeeded();
    const b = (await p.boundingBox())!;
    await page.mouse.move(b.x + 40, b.y + 12);
    await page.mouse.down();
    await page.mouse.move(b.x + 260, b.y + 12, { steps: 8 });
    await page.mouse.up();
    await clickBar(page, 'Highlight');
    const h = await one(page);
    expect(
      (await marks(page).count()) >= 1 && !!h && h.quote.length > 10,
      'a real mouse drag can be highlighted',
    );
    await page.context().close();
  }

  // Backups carry highlights
  {
    const mine: Stored = {
      same0001: {
        topic: TOPIC,
        quote: 'window',
        note: 'older',
        at: now - 9000,
        prefix: '',
        suffix: '',
      } as Stored[string],
    };
    const page = await open(browser, 'data/', {}, mine);
    await page.waitForSelector('.dt-sections');
    expect(
      (await page.innerText('.dt-sections')).includes('1 highlight'),
      'the data page counts highlights',
    );
    const bundle = (highlights: unknown) =>
      JSON.stringify({
        app: 'engineering-field-guide',
        schemaVersion: 1,
        exportedAt: new Date().toISOString(),
        data: { highlights },
      });
    const load = (content: string) =>
      page.setInputFiles('.dt input[type=file]', {
        name: 'b.json',
        mimeType: 'application/json',
        buffer: Buffer.from(content),
      });
    await load(
      bundle({
        same0001: {
          topic: TOPIC,
          quote: 'window',
          note: 'newer',
          at: now - 10,
          prefix: '',
          suffix: '',
        },
        more0002: { topic: TOPIC, quote: 'array', at: now - 20, prefix: '', suffix: '' },
      }),
    );
    await page.waitForSelector('.dt-preview');
    await page.click('text=Merge into my data');
    await page.waitForFunction(() =>
      document.querySelector('.dt-status')?.textContent?.startsWith('Imported'),
    );
    const merged = await stored(page);
    expect(
      merged.same0001!.note === 'newer' && !!merged.more0002,
      'merge combines highlights, newest wins',
    );
    await load(
      bundle({
        same0001: { topic: TOPIC, quote: 'window', at: now + 90 * 864e5, prefix: '', suffix: '' },
      }),
    );
    await page.waitForSelector('.dt-error');
    expect(
      (await page.innerText('.dt-error')).includes('Highlights'),
      'a damaged highlight is rejected by name',
    );
    await page.context().close();
  }
} finally {
  await browser.close();
}
console.log(
  failures.length ? `\n${failures.length} failed:\n${failures.join('\n')}` : '\nall passed',
);
process.exit(failures.length ? 1 : 0);

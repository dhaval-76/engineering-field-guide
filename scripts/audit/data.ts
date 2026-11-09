/**
 * End-to-end test of topic notes, the Your data page and the backup reminder, in fresh browser
 * profiles: export, preview, merge, replace, rejected files, sync codes, delete and rollback.
 *   npm run build && npm run preview   (in another terminal)
 *   npm run test:data
 */
import { readFileSync } from 'node:fs';
import { chromium, type Browser, type Page } from 'playwright';
import { BASE } from './pages';

const failures: string[] = [];
const expect = (ok: boolean, what: string) => {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${what}`);
  if (!ok) failures.push(what);
};
const DAY = 864e5;
const now = Date.now();
const A = 'dsa/sliding-window';
const B = 'dsa/two-pointers';
const C = 'dsa/binary-search';

async function fresh(browser: Browser, seed: Record<string, unknown> = {}): Promise<Page> {
  const ctx = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 375, height: 800 },
  });
  await ctx.addInitScript('globalThis.__name = (f) => f');
  // Block the worker so a test never waits on the offline download.
  await ctx.route('**/sw.js', (r) => r.abort());
  const page = await ctx.newPage();
  page.on('pageerror', (e) => failures.push(`page error: ${e.message}`));
  await page.goto(`${BASE}shortcuts/`);
  await page.evaluate((s) => {
    for (const [k, v] of Object.entries(s)) localStorage.setItem(`efg:${k}`, JSON.stringify(v));
  }, seed);
  await page.goto(`${BASE}data/`);
  await page.waitForSelector('.dt-sections');
  return page;
}
const stored = <T>(page: Page, key: string) =>
  page.evaluate((k) => JSON.parse(localStorage.getItem(`efg:${k}`) ?? 'null'), key) as Promise<T>;
const backup = (data: unknown, extra: object = {}) => ({
  app: 'engineering-field-guide',
  schemaVersion: 1,
  exportedAt: new Date(now).toISOString(),
  data,
  ...extra,
});
const open = (page: Page, content: string) =>
  page.setInputFiles('.dt input[type=file]', {
    name: 'backup.json',
    mimeType: 'application/json',
    buffer: Buffer.from(content),
  });
const cells = async (page: Page, label: string) =>
  (await page.locator('.dt-table tbody tr', { hasText: label }).locator('td').allInnerTexts()).join(
    ',',
  );

const browser = await chromium.launch();
try {
  // Notes
  {
    const page = await fresh(browser);
    await page.goto(`${BASE}${A}/`);
    await page.fill('[data-note] textarea', 'Shrink from the left.');
    await page.waitForFunction(() =>
      document.querySelector('[data-note-status]')?.textContent?.includes('Saved'),
    );
    await page.reload();
    expect(
      (await page.inputValue('[data-note] textarea')) === 'Shrink from the left.',
      'a note is kept after reload',
    );
    await page.fill('[data-note] textarea', '   ');
    await page.locator('[data-note] textarea').blur();
    expect(
      Object.keys(await stored<object>(page, 'notes')).length === 0,
      'blanking a note deletes it',
    );
    await page.context().close();
  }

  const mine = {
    progress: { [A]: now - 5 * DAY, [B]: now - 2 * DAY },
    bookmarks: [A],
    recent: [{ id: A, title: 'Sliding Window', at: now - DAY }],
    quiz: { [`${A}#abc`]: { box: 2, due: now + DAY, last: now - 3 * DAY, right: 2, wrong: 1 } },
    notes: { [A]: { text: 'mine', at: now - 9 * DAY } },
    theme: 'dark',
  };
  const theirs = {
    progress: { [A]: now - DAY, [C]: now - 3 * DAY },
    bookmarks: [B],
    recent: [{ id: B, title: 'Two Pointers', at: now - 2 * DAY }],
    quiz: { [`${A}#abc`]: { box: 0, due: now - DAY, last: now - DAY, right: 2, wrong: 2 } },
    notes: { [A]: { text: 'theirs', at: now - DAY } },
    preferences: { theme: 'light', lang: 'python' },
  };

  // View and export
  {
    const page = await fresh(browser, mine);
    const text = await page.innerText('.dt-sections');
    expect(
      text.includes('2 topics completed') && text.includes('Theme: dark'),
      'each section is described in plain words',
    );
    await page
      .locator('.dt-sections li', { hasText: 'Reading history' })
      .locator('input')
      .first()
      .uncheck();
    const [file] = await Promise.all([
      page.waitForEvent('download'),
      page.click('text=Download backup'),
    ]);
    const out = JSON.parse(readFileSync(await file.path(), 'utf8'));
    expect(
      out.app === 'engineering-field-guide' && out.schemaVersion === 1 && !!out.exportedAt,
      'export has app, version and date',
    );
    expect(
      !!out.data.progress && out.data.recent === undefined,
      'export leaves out unticked sections',
    );
    expect((await stored<number>(page, 'last-export')) > 0, 'export records the backup time');
    await page.context().close();
  }

  // Merge
  {
    const page = await fresh(browser, mine);
    await open(page, JSON.stringify(backup(theirs)));
    await page.waitForSelector('.dt-preview');
    expect(
      (await cells(page, 'Completed topics')) === '2,3,1,1,0',
      'preview counts a merge (now, after, new, updated, removed)',
    );
    expect(
      (await stored<object>(page, 'progress')) !== null &&
        Object.keys(await stored<object>(page, 'progress')).length === 2,
      'preview changes nothing',
    );
    await page.click('text=Merge into my data');
    await page.waitForFunction(() =>
      document.querySelector('.dt-status')?.textContent?.startsWith('Imported'),
    );
    const p = await stored<Record<string, number>>(page, 'progress');
    expect(
      p[A] === theirs.progress[A] && p[B] === mine.progress[B] && p[C] === theirs.progress[C],
      'merge combines progress, newest wins',
    );
    expect((await stored<string[]>(page, 'bookmarks')).length === 2, 'merge combines bookmarks');
    expect(
      (await stored<Record<string, { wrong: number }>>(page, 'quiz'))[`${A}#abc`]!.wrong === 2,
      'merge keeps the newer quiz record',
    );
    expect(
      (await stored<Record<string, { text: string }>>(page, 'notes'))[A]!.text === 'theirs',
      'merge keeps the newer note',
    );
    expect(
      (await stored<string>(page, 'theme')) === 'dark' &&
        (await stored<string>(page, 'lang')) === 'python',
      "merge keeps this device's preferences and fills gaps",
    );
    await page.context().close();
  }

  // Replace, section choice
  {
    const page = await fresh(browser, mine);
    await open(page, JSON.stringify(backup(theirs)));
    await page.waitForSelector('.dt-preview');
    await page.check('input[name=dt-mode] >> nth=1');
    await page.locator('.dt-table tr', { hasText: 'Saved topics' }).locator('input').uncheck();
    await page.locator('.dt-table tr', { hasText: 'Preferences' }).locator('input').uncheck();
    expect(
      (await cells(page, 'Completed topics')).endsWith(',1'),
      'preview counts what replace removes',
    );
    await page.click('text=Replace my data');
    await page.waitForFunction(() =>
      document.querySelector('.dt-status')?.textContent?.startsWith('Imported'),
    );
    expect(
      Object.keys(await stored<object>(page, 'progress'))
        .sort()
        .join() === [C, A].sort().join(),
      'replace makes the section match the backup',
    );
    expect(
      (await stored<string[]>(page, 'bookmarks')).join() === A,
      'unticked sections are left alone',
    );
    await page.context().close();
  }

  // Rejected input
  {
    const page = await fresh(browser, mine);
    const rejected = async (content: string, phrase: string, what: string) => {
      await open(page, content);
      await page.waitForSelector('.dt-error');
      const msg = await page.innerText('.dt-error');
      expect(msg.includes(phrase) && (await page.locator('.dt-preview').count()) === 0, what);
    };
    await rejected('not json', 'not valid JSON', 'rejects a file that is not JSON');
    await rejected(
      JSON.stringify({ app: 'other', schemaVersion: 1, data: {} }),
      'not a Field Guide backup',
      "rejects another app's file",
    );
    await rejected(
      JSON.stringify(backup(theirs, { schemaVersion: 2 })),
      'newer version',
      'rejects a newer format with a clear message',
    );
    await rejected(
      JSON.stringify(backup(theirs, { schemaVersion: 0 })),
      'unknown format',
      'rejects an unknown format',
    );
    await rejected(
      JSON.stringify(backup({ progress: { [A]: 'yesterday' } })),
      'Completed topics',
      'names the damaged section',
    );
    await rejected(
      JSON.stringify(backup({ progress: { [A]: now + 90 * DAY } })),
      'Completed topics',
      'rejects a far-future date that would always win a merge',
    );
    await rejected(
      `{"app":"engineering-field-guide","schemaVersion":1,"exportedAt":"2026-01-01T00:00:00Z","data":{"progress":{"__proto__":1}}}`,
      'Completed topics',
      'rejects a __proto__ key',
    );
    expect(
      Object.keys(await stored<object>(page, 'progress')).length === 2,
      'rejected files change nothing',
    );
    expect(
      await page.evaluate(() => ({}) as { polluted?: unknown }).then((o) => !('polluted' in o)),
      'object prototype is untouched',
    );
    await page.context().close();
  }

  // Refused write rolls back
  {
    const page = await fresh(browser, mine);
    await open(page, JSON.stringify(backup(theirs)));
    await page.waitForSelector('.dt-preview');
    await page.evaluate(() => {
      const real = Storage.prototype.setItem;
      let calls = 0;
      Storage.prototype.setItem = function (k: string, v: string) {
        if (++calls === 3) {
          Storage.prototype.setItem = real;
          throw new DOMException('full', 'QuotaExceededError');
        }
        real.call(this, k, v);
      };
    });
    await page.click('text=Merge into my data');
    await page.waitForSelector('.dt-error');
    const p = await stored<Record<string, number>>(page, 'progress');
    expect(
      p[A] === mine.progress[A] && Object.keys(p).length === 2,
      'a refused write puts everything back',
    );
    await page.context().close();
  }

  // Sync code and QR link
  {
    const source = await fresh(browser, mine);
    await source.click('text=Copy sync code');
    await source.waitForSelector('.dt-code');
    const code = await source.inputValue('.dt-code');
    expect(code.startsWith('EFG1.'), 'a sync code is produced');
    const target = await fresh(browser, { bookmarks: [C] });
    await target.fill('.dt-paste textarea', code);
    await target.click('.dt-paste button');
    await target.waitForSelector('.dt-preview');
    await target.click('text=Merge into my data');
    await target.waitForFunction(() =>
      document.querySelector('.dt-status')?.textContent?.startsWith('Imported'),
    );
    expect(
      (await stored<string[]>(target, 'bookmarks')).length === 2,
      'a pasted code merges on another device',
    );
    await target.fill('.dt-paste textarea', `${code.slice(0, 40)}!!`);
    await target.click('.dt-paste button');
    await target.waitForSelector('.dt-error');
    expect((await target.innerText('.dt-error')).includes('damaged'), 'a damaged code is rejected');

    const link = await fresh(browser);
    await link.goto(`${BASE}data/#sync=${code}`);
    await link.waitForSelector('.dt-preview');
    expect(!link.url().includes('sync='), 'a sync link is removed from the address bar');
    expect(
      (await stored<unknown>(link, 'progress')) === null,
      'a sync link only previews until confirmed',
    );

    await source
      .locator('.dt-sections li', { hasText: 'Quiz answers' })
      .locator('input')
      .first()
      .uncheck();
    await source.click('text=Show QR code');
    await source.waitForSelector('.dt-qr svg path');
    expect(true, 'a small selection shows a QR code');
    const big = Object.fromEntries(
      Array.from({ length: 600 }, (_, i) => [
        `dsa/t${(i * 7919).toString(36)}`,
        now - i * 1000 - 1,
      ]),
    );
    const large = await fresh(browser, { progress: big });
    await large.click('text=Show QR code');
    await large.waitForSelector('text=too much data');
    expect(
      (await large.locator('.dt-qr').count()) === 0,
      'too much data explains itself instead of a dense QR',
    );
    for (const p of [source, target, link, large]) await p.context().close();
  }

  // Delete
  {
    const page = await fresh(browser, mine);
    page.on('dialog', (d) => d.accept());
    await page
      .locator('.dt-sections > li', { hasText: 'Saved topics' })
      .locator('button.danger')
      .click();
    expect((await stored<unknown>(page, 'bookmarks')) === null, 'a section can be deleted');
    expect(
      (await stored<unknown>(page, 'progress')) !== null,
      'deleting a section leaves the others',
    );
    await Promise.all([
      page.waitForEvent('load'),
      page.click('.dt-actions >> text=Delete everything'),
    ]);
    const left = await page.evaluate(() =>
      Object.keys(localStorage).filter((k) => k.startsWith('efg:')),
    );
    expect(left.length === 0, 'delete everything clears every stored key');
    await page.context().close();
  }

  // Backup reminder
  {
    const quiet = await fresh(browser, { progress: { [A]: now - 5 * DAY } });
    await quiet.goto(BASE);
    await quiet.waitForTimeout(800);
    expect((await quiet.locator('.dt-reminder').count()) === 0, 'no reminder for recent progress');
    const page = await fresh(browser, { progress: { [A]: now - 40 * DAY } });
    await page.goto(BASE);
    await page.waitForSelector('.dt-reminder');
    expect(true, 'overdue progress shows the reminder');
    await page.click('.dt-reminder button');
    await page.reload();
    await page.waitForTimeout(800);
    expect((await page.locator('.dt-reminder').count()) === 0, 'Not now snoozes the reminder');
    const saved = await fresh(browser, {
      progress: { [A]: now - 40 * DAY },
      'last-export': now - 39 * DAY,
    });
    await saved.goto(BASE);
    await saved.waitForTimeout(800);
    expect(
      (await saved.locator('.dt-reminder').count()) === 0,
      'no reminder when nothing is new since the backup',
    );
    for (const p of [quiet, page, saved]) await p.context().close();
  }
} finally {
  await browser.close();
}
console.log(
  failures.length ? `\n${failures.length} failed:\n${failures.join('\n')}` : '\nall passed',
);
process.exit(failures.length ? 1 : 0);

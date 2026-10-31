/**
 * Renders the app icons used by the web app manifest and iOS home screen into public/icons/.
 *   npm run icons     (run after changing the logo; the PNGs are committed)
 */
import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';

const outDir = new URL('../../public/icons/', import.meta.url).pathname;

// The header mark on a paper-coloured square. `inset` leaves the safe zone that maskable icons need.
const mark = (size: number, inset: number, radius: number) => `<!doctype html>
<html><body style="margin:0">
<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="${radius}" fill="#f8f6f1"/>
  <g transform="translate(${inset} ${inset}) scale(${(100 - 2 * inset) / 28})">
    <rect x="1.5" y="1.5" width="25" height="25" rx="5" fill="none" stroke="#1c1b19" stroke-width="1.5"/>
    <path d="M8 19V9h8M8 14h6" fill="none" stroke="#a2401c" stroke-width="2.2" stroke-linecap="round"/>
    <circle cx="19.5" cy="18.5" r="2" fill="#a2401c"/>
  </g>
</svg></body></html>`;

const icons = [
  { name: 'icon-192.png', size: 192, inset: 8, radius: 18 },
  { name: 'icon-512.png', size: 512, inset: 8, radius: 18 },
  // Maskable: full-bleed background, logo inside the central 80% circle.
  { name: 'maskable-512.png', size: 512, inset: 22, radius: 0 },
  // iOS rounds the corners itself.
  { name: 'apple-touch-icon.png', size: 180, inset: 12, radius: 0 },
];

await mkdir(outDir, { recursive: true });
const browser = await chromium.launch();
for (const { name, size, inset, radius } of icons) {
  const page = await browser.newPage({ viewport: { width: size, height: size } });
  await page.setContent(mark(size, inset, radius));
  await page.screenshot({ path: join(outDir, name), omitBackground: true });
  await page.close();
}
await browser.close();
console.log(`icons: ${icons.length} written to public/icons/`);

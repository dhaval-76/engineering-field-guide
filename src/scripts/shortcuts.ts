/** The `?` shortcuts panel and the small confirmation shown after a shortcut acts off-screen. */
import { shortcutGroups } from '~/lib/shortcuts';

const BASE = import.meta.env.BASE_URL.replace(/\/?$/, '/');
const esc = (s: string) => s.replace(/[&<>"]/g, (c) => `&#${c.charCodeAt(0)};`);
const keyHtml = (combo: string) =>
  combo
    .split(' ')
    .map((k) => `<kbd>${esc(k)}</kbd>`)
    .join(combo.startsWith('Ctrl') ? ' ' : ' <span class="then">then</span> ');

let panel: HTMLDialogElement | null = null;
export function openShortcuts() {
  if (!panel) {
    panel = document.createElement('dialog');
    panel.className = 'shortcuts-panel';
    panel.setAttribute('aria-labelledby', 'shortcuts-title');
    panel.innerHTML = `
      <div class="sp-head">
        <h2 id="shortcuts-title">Keyboard shortcuts</h2>
        <button type="button" data-close aria-label="Close">Close</button>
      </div>
      <div class="sp-body">${shortcutGroups
        .map(
          (g) =>
            `<section><h3>${esc(g.title)}${g.where ? ` <span>${esc(g.where)}</span>` : ''}</h3><dl>${g.items
              .map(
                (s) =>
                  `<div><dt>${s.keys.map(keyHtml).join(' <span class="or">/</span> ')}</dt><dd>${esc(s.does)}</dd></div>`,
              )
              .join('')}</dl></section>`,
        )
        .join('')}</div>
      <p class="sp-foot">Letter shortcuts are ignored while you type in a field. <a href="${BASE}shortcuts/">All shortcuts</a></p>`;
    document.body.append(panel);
    panel.addEventListener('click', (e) => {
      const t = e.target as HTMLElement;
      if (t === panel || t.closest('[data-close]')) panel!.close();
    });
  }
  if (!panel.open) panel.showModal();
}

let toast: HTMLElement | null = null;
let hide: ReturnType<typeof setTimeout> | undefined;
/** Briefly confirms what a shortcut did, for changes that may be off-screen. */
export function flash(message: string) {
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'key-toast';
    toast.setAttribute('role', 'status');
    document.body.append(toast);
  }
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(hide);
  hide = setTimeout(() => (toast!.hidden = true), 1600);
}

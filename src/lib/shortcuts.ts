/**
 * Every keyboard shortcut on the site, in one list. The `?` panel (src/scripts/shortcuts.ts) and the
 * /shortcuts/ page both render from it, so update this when a key is added or changed.
 * Single-key shortcuts are ignored while typing in a field.
 */
export interface Shortcut {
  /** Keys pressed together; a space inside one entry means "then" (g then h). */
  keys: string[];
  does: string;
}
export interface ShortcutGroup {
  title: string;
  where?: string;
  items: Shortcut[];
}

export const shortcutGroups: ShortcutGroup[] = [
  {
    title: 'Anywhere',
    items: [
      { keys: ['?'], does: 'Show these shortcuts' },
      { keys: ['/', 'Ctrl K'], does: 'Search (⌘ K on a Mac)' },
      { keys: ['t'], does: 'Switch between light and dark theme' },
      { keys: ['g h'], does: 'Go to the home page' },
      { keys: ['g p'], does: 'Go to your progress' },
      { keys: ['g r'], does: 'Go to review' },
      { keys: ['g o'], does: 'Go to the offline page' },
      { keys: ['Esc'], does: 'Close search, the menu, a diagram or this panel' },
    ],
  },
  {
    title: 'Search',
    where: 'while the search box is open',
    items: [
      { keys: ['↑', '↓'], does: 'Move through the results' },
      { keys: ['Enter'], does: 'Open the selected result' },
    ],
  },
  {
    title: 'On a topic',
    items: [
      { keys: ['j'], does: 'Next topic' },
      { keys: ['k'], does: 'Previous topic' },
      { keys: [']'], does: 'Next section on the page' },
      { keys: ['['], does: 'Previous section on the page' },
      { keys: ['m'], does: 'Mark complete, or not complete' },
      { keys: ['b'], does: 'Save or unsave the topic' },
      { keys: ['r'], does: 'Reading mode: hide the side panels' },
    ],
  },
  {
    title: 'Code',
    items: [
      { keys: ['1', '2', '3', '4', '5'], does: 'Show Java, JavaScript, Python, TypeScript or Go' },
      { keys: ['w'], does: 'Wrap long code lines, or stop wrapping' },
      { keys: ['←', '→'], does: 'Switch language tab (when a tab has focus)' },
    ],
  },
  {
    title: 'Walkthroughs and diagrams',
    items: [
      { keys: ['←', '→'], does: 'Previous or next step (when the walkthrough has focus)' },
      { keys: ['Esc'], does: 'Close the full-screen diagram' },
    ],
  },
  {
    title: 'Review',
    where: 'on the review page',
    items: [
      { keys: ['1', '2', '3', '4'], does: 'Pick an answer' },
      { keys: ['Enter'], does: 'Next question' },
    ],
  },
];

/** `g` then this key goes to the page (paths relative to the site root). */
export const goTo: Record<string, string> = {
  h: '',
  p: 'progress/',
  r: 'review/',
  o: 'offline/',
};

/** Number keys 1–5 switch code tabs to these languages (ids from codeLanguages in site.config.ts). */
export const languageKeys = ['java', 'javascript', 'python', 'typescript', 'go'];

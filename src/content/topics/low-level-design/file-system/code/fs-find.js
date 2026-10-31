// Filters as plain functions composed with and/or/not.
const globToRegex = (glob) =>
  new RegExp(
    '^' +
      glob
        .replace(/[.+^${}()|[\]\\]/g, '\\$&')
        .replace(/\*/g, '[^/]*')
        .replace(/\?/g, '[^/]') +
      '$',
  );

export const name = (glob) => {
  const re = globToRegex(glob);
  return (n) => re.test(n.name);
};
export const isDir = (n) => n.type === 'dir';
export const minSize = (bytes) => (n) => n.type === 'file' && n.size >= bytes;
export const and =
  (...fs) =>
  (n) =>
    fs.every((f) => f(n));
export const or =
  (...fs) =>
  (n) =>
    fs.some((f) => f(n));
export const not = (f) => (n) => !f(n);

export function* find(node, filter) {
  const stack = [node];
  while (stack.length) {
    const n = stack.pop();
    if (filter(n)) yield n;
    if (n.type === 'dir') stack.push(...n.children);
  }
}

// [...find(root, and(name('*.java'), minSize(1024), not(name('*Test*'))))]

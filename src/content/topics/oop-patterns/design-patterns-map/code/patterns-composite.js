// Composite: a single item and a group of items answer the same question the same way.
const fileLeaf = (name, sizeBytes) => ({ name, sizeBytes: () => sizeBytes });
const folder = (name, children) => ({ name, sizeBytes: () => children.reduce((s, c) => s + c.sizeBytes(), 0) });
// folder('src', [fileLeaf('a.js', 1200), folder('util', [fileLeaf('b.js',
// 300)])]).sizeBytes()  → 1500

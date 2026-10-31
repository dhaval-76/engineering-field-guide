// The same leak shapes exist in garbage-collected JS:
const cache = new Map();                       // ✗ unbounded module-level cache
// ✗ timer keeps `this` (and everything it references) alive
setInterval(() => poll(this), 1000);
// ✗ never removed → MaxListenersExceededWarning, leak
emitter.on('data', handler);

// WeakMap/WeakRef let values die with their keys — useful for metadata about objects you
// don't own:
const meta = new WeakMap();
// entry disappears when domNode is collected
meta.set(domNode, { clicks: 0 });

// The same leak shapes exist in garbage-collected JS:
const cache = new Map();                       // ✗ unbounded module-level cache
setInterval(() => poll(this), 1000);           // ✗ timer keeps `this` (and everything it references) alive
emitter.on('data', handler);                   // ✗ never removed → MaxListenersExceededWarning, leak

// WeakMap/WeakRef let values die with their keys — useful for metadata about objects you don't own:
const meta = new WeakMap();
meta.set(domNode, { clicks: 0 });              // entry disappears when domNode is collected

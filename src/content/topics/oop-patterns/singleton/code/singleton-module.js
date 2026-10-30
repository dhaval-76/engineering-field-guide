// An ES module is evaluated once per process and cached, so a module-level object
// is already a singleton. No class gymnastics needed.
// config.js
const config = Object.freeze({
  dbUrl: process.env.DB_URL,
  poolSize: Number(process.env.POOL_SIZE ?? 10),
});
export default config;

// Lazily created shared resource:
let pool;
export function getPool() {
  pool ??= createPool(config); // JS runs one thread per event loop, so no race here
  return pool;
}

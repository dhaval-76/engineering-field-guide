// JS uses optional chaining (?.) and nullish coalescing (??) instead of a wrapper type.
const city = user?.profile?.address?.city ?? 'Unknown';

// ?? only falls back on null/undefined — unlike ||, it keeps 0, '' and false:
const retries = config.retries ?? 3;   // 0 stays 0
const bad = config.retries || 3;       // 0 becomes 3 — usually a bug

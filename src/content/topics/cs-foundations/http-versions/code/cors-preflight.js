/** Request headers a page may send cross-origin without a preflight (Fetch standard). */
const SAFELISTED = new Set(['accept', 'accept-language', 'content-language', 'content-type', 'range']);
const SIMPLE_TYPES = new Set([
  'application/x-www-form-urlencoded',
  'multipart/form-data',
  'text/plain',
]);

/** Does a browser send an OPTIONS preflight before this cross-origin fetch? */
function needsPreflight(method, headers) {
  if (!['GET', 'HEAD', 'POST'].includes(method)) return true;
  for (const [key, value] of Object.entries(headers)) {
    const name = key.toLowerCase();
    if (!SAFELISTED.has(name)) return true; // e.g. Authorization, X-Request-Id
    if (name === 'content-type') {
      const type = value.split(';')[0].trim().toLowerCase();
      if (!SIMPLE_TYPES.has(type)) return true; // e.g. application/json
    }
    if (name === 'range' && !/^bytes=\d+-\d*$/.test(value)) return true;
  }
  return false;
}

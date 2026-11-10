import { timingSafeEqual } from 'node:crypto';

const ENTITIES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#x27;' };

/** Encodes text for an HTML body or a quoted attribute, so it can never become markup. */
function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ENTITIES[c]);
}

/**
 * CSRF check for a state-changing request. The browser says where the request came from
 * (Sec-Fetch-Site, or Origin in older browsers), and the form must echo the session's token.
 * Header names are lower-case here, as Node gives them.
 */
function allowStateChange(method, headers, ourOrigin, sessionToken, sentToken) {
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) return true; // must change nothing
  const site = headers['sec-fetch-site'];
  if (site !== undefined) {
    if (site !== 'same-origin' && site !== 'none') return false; // another site
  } else if (headers.origin !== ourOrigin) {
    return false;
  }
  if (typeof sentToken !== 'string') return false;
  const a = Buffer.from(sessionToken);
  const b = Buffer.from(sentToken);
  return a.length === b.length && timingSafeEqual(a, b); // constant-time comparison
}

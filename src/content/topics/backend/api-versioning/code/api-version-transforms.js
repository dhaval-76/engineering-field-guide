/**
 * The code only ever builds the newest response. Older clients get it walked back through each
 * change newer than their pinned version, so old versions cost one small function each.
 *
 * Each change: { date, description, downgrade(response) → response }, newest first.
 */
function renderFor(latest, clientVersion, newestFirst) {
  let response = { ...latest };
  for (const change of newestFirst) {
    if (change.date <= clientVersion) break;
    response = change.downgrade(response);
  }
  return response;
}

// A rank strictly between two ranks, using lowercase letters as base-26 digits.
// `after === null` means "end of column". Generated ranks never end in 'a', so there is always room.
export function rankBetween(before, after) {
  let out = '';
  for (let i = 0; ; i++) {
    const lo = i < before.length ? before.charCodeAt(i) - 97 : 0;
    const hi = after !== null && i < after.length ? after.charCodeAt(i) - 97 : 26;
    if (hi - lo > 1) return out + String.fromCharCode(97 + Math.floor((lo + hi) / 2)); // room: take the midpoint
    out += String.fromCharCode(97 + lo);       // no room: keep this digit and look one position deeper
    if (hi - lo === 1) after = null;           // now below `after`, so the upper bound opens up
  }
}
// rankBetween('c', 'm') → 'h';  rankBetween('h', 'i') → 'hn';  rankBetween('', 'b') → 'an';  rankBetween('t', null) → 'w'

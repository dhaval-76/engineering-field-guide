// npm i luxon — JavaScript Date has no time-zone arithmetic; Luxon keeps 10:00 at 10:00 across DST.
import { DateTime } from 'luxon';

// rule: { firstDay: '2026-10-19', until: '2026-11-13', weekdays: [1, 3] (1 = Monday), start: '10:00', minutes: 30, zone }
export function expand(rule) {
  const out = [];
  const [h, m] = rule.start.split(':').map(Number);
  const last = DateTime.fromISO(rule.until, { zone: rule.zone });
  for (let d = DateTime.fromISO(rule.firstDay, { zone: rule.zone }); d <= last; d = d.plus({ days: 1 })) {
    if (!rule.weekdays.includes(d.weekday)) continue;
    const start = d.set({ hour: h, minute: m });
    out.push({ start: start.toMillis(), end: start.plus({ minutes: rule.minutes }).toMillis() });
  }
  return out;
}

// All-or-nothing: Node runs this synchronously, so nothing can book in between the checks and the inserts.
export function bookSeries(room, rule, organizer, attendees = []) {
  const seriesId = crypto.randomUUID();
  const slots = expand(rule);
  const clashes = slots.filter((s) => !room.isFree(s));
  if (clashes.length) throw Object.assign(new Error(`${clashes.length} occurrences clash`), { clashes });
  return slots.map((s) => room.book(s, organizer, attendees, seriesId));
}

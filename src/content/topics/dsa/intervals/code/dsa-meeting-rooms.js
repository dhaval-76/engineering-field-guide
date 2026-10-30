function minMeetingRooms(meetings) {
  const starts = meetings.map((m) => m[0]).sort((a, b) => a - b);
  const ends = meetings.map((m) => m[1]).sort((a, b) => a - b);
  let rooms = 0, best = 0, e = 0;
  for (const s of starts) {
    while (e < ends.length && ends[e] <= s) { e++; rooms--; }
    best = Math.max(best, ++rooms);
  }
  return best;
}

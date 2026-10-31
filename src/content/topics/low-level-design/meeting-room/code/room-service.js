// uses Room and SlotTakenError from above
export class RoomFinder {
  constructor(rooms) {
    this.rooms = [...rooms];
  }

  // Smallest room that fits — keeps big rooms free for big meetings.
  candidates(slot, people, needs = []) {
    return this.rooms
      .filter((r) => r.capacity >= people && needs.every((n) => r.amenities.has(n)) && r.isFree(slot))
      .sort((a, b) => a.capacity - b.capacity);
  }

  // With several processes (or an async gap) another booking can win between search and book.
  bookBest(slot, people, needs, organizer, attendees = []) {
    for (const room of this.candidates(slot, people, needs)) {
      try {
        return room.book(slot, organizer, attendees);
      } catch (e) {
        if (!(e instanceof SlotTakenError)) throw e; // raced: try the next room
      }
    }
    throw new Error('no room available');
  }
}

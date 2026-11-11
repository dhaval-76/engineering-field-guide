record Song(String id, String title, String genre, boolean explicit) {}

interface SongSource {
    Optional<Song> next();
} // the DJ service, the recommender

/** Takes `a` songs from the first source, then `b` from the second, and repeats. */
class Mixer implements Iterator<Song> {
    private final SongSource first, second;
    private final int a, b;
    private final Predicate<Song> filter;
    private final Set<String> played = new HashSet<>(); // no repeats in one playlist
    private int position;
    private Song nextSong;

    Mixer(SongSource first, SongSource second, int a, int b, List<Predicate<Song>> filters) {
        if (a < 0 || b < 0 || a + b == 0) throw new IllegalArgumentException("bad ratio");
        this.first = first;
        this.second = second;
        this.a = a;
        this.b = b;
        this.filter = filters.stream().reduce(s -> true, Predicate::and);
    }

    public boolean hasNext() {
        if (nextSong == null) nextSong = fetch();
        return nextSong != null;
    }

    public Song next() {
        if (!hasNext()) throw new NoSuchElementException();
        Song s = nextSong;
        nextSong = null;
        return s;
    }

    private Song fetch() {
        boolean firstExhausted = false, secondExhausted = false;
        while (!(firstExhausted && secondExhausted)) {
            boolean fromFirst = position % (a + b) < a;
            if (fromFirst && firstExhausted) fromFirst = false; // one ran dry: use the other
            if (!fromFirst && secondExhausted) fromFirst = true;
            Optional<Song> s = (fromFirst ? first : second).next();
            if (s.isEmpty()) {
                if (fromFirst) firstExhausted = true;
                else secondExhausted = true;
                continue;
            }
            Song song = s.get();
            if (!filter.test(song) || !played.add(song.id())) continue; // skip, same slot
            position++;
            return song;
        }
        return null;
    }
}

final class SongFilters {
    static Predicate<Song> noExplicit() {
        return s -> !s.explicit();
    }

    static Predicate<Song> excludeGenres(Set<String> genres) {
        return s -> !genres.contains(s.genre());
    }
}

interface Command {
    void execute(Buffer buf);
    void undo(Buffer buf);
    /** Merge a follow-up command into this one (e.g. consecutive keystrokes) — returns false if it can't. */
    default boolean absorb(Command next) { return false; }
}

final class InsertText implements Command {
    private final int pos;
    private final StringBuilder text;
    private final Instant at;
    InsertText(int pos, String text, Instant at) { this.pos = pos; this.text = new StringBuilder(text); this.at = at; }

    public void execute(Buffer b) { b.insert(pos, text.toString()); }
    public void undo(Buffer b)    { b.delete(pos, text.length()); }

    /** Typing "h","e","l","l","o" quickly becomes ONE undo step, like real editors. */
    public boolean absorb(Command next) {
        if (next instanceof InsertText n && n.pos == pos + text.length()
                && Duration.between(at, n.at).toMillis() < 1000 && !n.text.toString().contains(" ")) {
            text.append(n.text);
            return true;
        }
        return false;
    }
}

final class DeleteText implements Command {
    private final int pos, len;
    private String removed;                                       // captured on execute, needed for undo
    DeleteText(int pos, int len) { this.pos = pos; this.len = len; }
    public void execute(Buffer b) { removed = b.delete(pos, len); }
    public void undo(Buffer b)    { b.insert(pos, removed); }
}

final class Editor {
    private final Buffer buffer;
    private final Deque<Command> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final int maxHistory;

    Editor(Buffer buffer, int maxHistory) { this.buffer = buffer; this.maxHistory = maxHistory; }

    void apply(Command c) {
        c.execute(buffer);                     // the command applies itself once…
        Command top = undo.peek();
        if (top == null || !top.absorb(c)) {   // …and is either merged into the previous step or pushed
            undo.push(c);
            if (undo.size() > maxHistory) undo.removeLast();   // oldest history drops off
        }
        redo.clear();                          // a new edit invalidates the redo branch
    }

    boolean undo() {
        Command c = undo.poll();
        if (c == null) return false;
        c.undo(buffer);
        redo.push(c);
        return true;
    }

    boolean redo() {
        Command c = redo.poll();
        if (c == null) return false;
        c.execute(buffer);
        undo.push(c);
        return true;
    }
}

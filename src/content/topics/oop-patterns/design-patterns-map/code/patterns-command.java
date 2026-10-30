// Command: an action as an object — can be queued, logged, retried or undone.
interface Command { void execute(); void undo(); }

record InsertText(Document doc, int pos, String text) implements Command {
    public void execute() { doc.insert(pos, text); }
    public void undo()    { doc.delete(pos, text.length()); }
}

final class Editor {
    private final Deque<Command> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    void run(Command c) { c.execute(); undo.push(c); redo.clear(); }
    void undo() { if (!undo.isEmpty()) { Command c = undo.pop(); c.undo(); redo.push(c); } }
    void redo() { if (!redo.isEmpty()) { Command c = redo.pop(); c.execute(); undo.push(c); } }
}

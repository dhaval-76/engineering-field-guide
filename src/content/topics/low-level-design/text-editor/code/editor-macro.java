/**
 * A composite command: one undo step for many actions (paste-with-format,
 * find-and-replace-all, macros).
 */
final class CompositeCommand implements Command {
    private final List<Command> steps;
    CompositeCommand(List<Command> steps) { this.steps = List.copyOf(steps); }
    public void execute(Buffer b) { steps.forEach(s -> s.execute(b)); }
    public void undo(Buffer b) {
        // reverse order
        for (ListIterator<Command> it = steps.listIterator(steps.size()); it.hasPrevious(); ) it.previous().undo(b);
    }
}

/**
 * Macro recording: capture commands while recording, replay them later as one composite.
 */
final class MacroRecorder {
    private List<Command> recording;
    void start() { recording = new ArrayList<>(); }
    void capture(Command c) { if (recording != null) recording.add(c); }
    CompositeCommand stop() { var m = new CompositeCommand(recording); recording = null; return m; }
}

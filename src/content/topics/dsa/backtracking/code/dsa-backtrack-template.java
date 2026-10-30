void backtrack(State state, List<Result> results) {
    if (state.isComplete()) {
        results.add(state.snapshot());           // COPY — the state keeps changing
        return;
    }
    for (Choice c : state.choices()) {
        if (!state.isValid(c)) continue;         // prune
        state.apply(c);                          // choose
        backtrack(state, results);               // explore
        state.undo(c);                           // unchoose
    }
}

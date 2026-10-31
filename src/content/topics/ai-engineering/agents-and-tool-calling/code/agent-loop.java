// Provider-neutral types; a thin client maps them to the provider's tool-calling format.
record ToolCall(String id, String name, Map<String, Object> args) {}
// no tool calls = final answer
record Reply(String text, List<ToolCall> toolCalls) {}
record Msg(String role, String text, List<ToolCall> calls, String toolCallId) {}
record Tool(String name, String description, String jsonSchema, boolean changesData,
            Function<Map<String, Object>, String> run) {}

interface Model { Reply next(List<Msg> history, Collection<Tool> tools); }

class Agent {
    private final Model model;
    private final Map<String, Tool> tools;
    private final int maxSteps;

    Agent(Model model, List<Tool> tools, int maxSteps) {
        this.model = model; this.maxSteps = maxSteps;
        this.tools = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
    }

    /**
     * Runs the model ↔ tool loop. `mayRun` is the caller's authorization check, enforced
     * in code.
     */
    String run(String userRequest, BiPredicate<Tool, Map<String, Object>> mayRun) {
        List<Msg> history = new ArrayList<>(List.of(new Msg("user", userRequest, List.of(), null)));
        for (int step = 0; step < maxSteps; step++) {
            Reply reply = model.next(history, tools.values());
            history.add(new Msg("assistant", reply.text(), reply.toolCalls(), null));
            if (reply.toolCalls().isEmpty()) return reply.text();                  // done

            for (ToolCall call : reply.toolCalls()) {
                Tool tool = tools.get(call.name());
                String result;
                if (tool == null) result = "error: unknown tool " + call.name();
                else if (!mayRun.test(tool, call.args())) result = "error: not permitted";
                else {
                    try { result = tool.run().apply(call.args()); }
                    // let the model recover
                    catch (RuntimeException e) { result = "error: " + e.getMessage(); }
                }
                history.add(new Msg("tool", result, List.of(), call.id()));
            }
        }
        // budget, not an infinite loop
        return "Stopped after " + maxSteps + " steps without finishing.";
    }
}

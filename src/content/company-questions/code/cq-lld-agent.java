record Message(String role, String content) {} // user, assistant, tool

record ToolCall(String tool, String argument) {}

/** What the model returns: either a final answer or a request to call a tool. */
record ModelReply(String answer, ToolCall toolCall) {}

interface ModelClient {
    ModelReply complete(List<Message> conversation, Set<String> tools);
}

interface Tool {
    String name();

    String run(String argument) throws Exception;
}

class Agent {
    private final ModelClient model;
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final List<Message> memory = new ArrayList<>();
    private final int maxSteps, memoryLimit;

    Agent(ModelClient model, List<Tool> tools, int maxSteps, int memoryLimit) {
        this.model = model;
        for (Tool t : tools) this.tools.put(t.name(), t);
        this.maxSteps = maxSteps;
        this.memoryLimit = memoryLimit;
    }

    String handle(String userMessage) {
        remember(new Message("user", userMessage));
        for (int step = 0; step < maxSteps; step++) { // a hard stop: no endless loops
            ModelReply reply = model.complete(List.copyOf(memory), tools.keySet());
            if (reply.toolCall() == null) {
                remember(new Message("assistant", reply.answer()));
                return reply.answer();
            }
            remember(new Message("tool", runTool(reply.toolCall())));
        }
        return "Sorry, I couldn't finish that. Please try rephrasing.";
    }

    private String runTool(ToolCall call) {
        Tool tool = tools.get(call.tool());
        if (tool == null) return "error: unknown tool " + call.tool(); // let the model recover
        try {
            return tool.run(call.argument());
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    private void remember(Message m) {
        memory.add(m);
        while (memory.size() > memoryLimit) memory.remove(0); // or summarise old turns
    }
}

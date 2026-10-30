// model.next(history, tools) -> { text, toolCalls: [{ id, name, args }] }  (empty toolCalls = final answer)
// tool: { name, description, jsonSchema, changesData, run(args) -> string | Promise<string> }
export async function runAgent({ model, tools, maxSteps = 8 }, userRequest, mayRun) {
  const byName = new Map(tools.map((t) => [t.name, t]));
  const history = [{ role: 'user', text: userRequest }];

  for (let step = 0; step < maxSteps; step++) {
    const reply = await model.next(history, tools);
    history.push({ role: 'assistant', text: reply.text, calls: reply.toolCalls });
    if (reply.toolCalls.length === 0) return reply.text;

    for (const call of reply.toolCalls) {
      const tool = byName.get(call.name);
      let result;
      if (!tool) result = `error: unknown tool ${call.name}`;
      else if (!mayRun(tool, call.args)) result = 'error: not permitted';
      else {
        try {
          result = await tool.run(call.args);
        } catch (e) {
          result = `error: ${e.message}`;
        }
      }
      history.push({ role: 'tool', text: result, toolCallId: call.id });
    }
  }
  return `Stopped after ${maxSteps} steps without finishing.`;
}

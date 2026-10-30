from dataclasses import dataclass, field
from typing import Any, Callable, Protocol

@dataclass(frozen=True)
class ToolCall:
    id: str
    name: str
    args: dict[str, Any]

@dataclass(frozen=True)
class Reply:
    text: str
    tool_calls: list[ToolCall] = field(default_factory=list)   # empty = final answer

@dataclass(frozen=True)
class Tool:
    name: str
    description: str
    json_schema: str
    changes_data: bool
    run: Callable[[dict[str, Any]], str]

class Model(Protocol):
    def next(self, history: list[dict], tools: list[Tool]) -> Reply: ...

def run_agent(model: Model, tools: list[Tool], user_request: str,
              may_run: Callable[[Tool, dict], bool], max_steps: int = 8) -> str:
    by_name = {t.name: t for t in tools}
    history: list[dict] = [{"role": "user", "text": user_request}]
    for _ in range(max_steps):
        reply = model.next(history, tools)
        history.append({"role": "assistant", "text": reply.text, "calls": reply.tool_calls})
        if not reply.tool_calls:
            return reply.text
        for call in reply.tool_calls:
            tool = by_name.get(call.name)
            if tool is None:
                result = f"error: unknown tool {call.name}"
            elif not may_run(tool, call.args):
                result = "error: not permitted"
            else:
                try:
                    result = tool.run(call.args)
                except Exception as e:           # let the model see the error and recover
                    result = f"error: {e}"
            history.append({"role": "tool", "text": result, "tool_call_id": call.id})
    return f"Stopped after {max_steps} steps without finishing."

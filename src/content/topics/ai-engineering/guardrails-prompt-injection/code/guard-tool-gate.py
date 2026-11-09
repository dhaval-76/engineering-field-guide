from enum import Enum


class Risk(Enum):
    READ_PUBLIC = 1
    READ_PRIVATE = 2
    WRITE = 3
    SENDS_OUT = 4


class ToolGate:
    """Decides in code whether the model may use a tool.

    The model only proposes; it cannot talk its way past this, because this class never reads
    its reasoning.
    """

    def __init__(self, allowed_tools):
        self.allowed_tools = set(allowed_tools)
        self.saw_untrusted = False
        self.saw_private = False

    def record_untrusted_content(self):
        """Call for every tool result that an outsider could have influenced."""
        self.saw_untrusted = True

    def authorize(self, tool, risk):
        if tool not in self.allowed_tools:
            return "deny"
        if risk is Risk.READ_PUBLIC:
            return "allow"
        if risk is Risk.READ_PRIVATE:
            self.saw_private = True
            return "allow"
        # Private data + untrusted content + a way out is how data gets stolen.
        if risk is Risk.SENDS_OUT and self.saw_untrusted and self.saw_private:
            return "deny"
        return "ask_human" if self.saw_untrusted else "allow"

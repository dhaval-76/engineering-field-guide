enum ToolRisk {
    READ_PUBLIC,
    READ_PRIVATE,
    WRITE,
    SENDS_OUT
}

enum Decision {
    ALLOW,
    ASK_HUMAN,
    DENY
}

/**
 * Decides in code whether the model may use a tool. The model only proposes; it cannot talk its
 * way past this, because this class never reads its reasoning.
 */
class ToolGate {
    private final Set<String> allowedTools;
    private boolean sawUntrusted;
    private boolean sawPrivate;

    ToolGate(Set<String> allowedTools) {
        this.allowedTools = allowedTools;
    }

    /** Call for every tool result that an outsider could have influenced. */
    void recordUntrustedContent() {
        sawUntrusted = true;
    }

    Decision authorize(String tool, ToolRisk risk) {
        if (!allowedTools.contains(tool)) return Decision.DENY;
        switch (risk) {
            case READ_PUBLIC -> {
                return Decision.ALLOW;
            }
            case READ_PRIVATE -> {
                sawPrivate = true;
                return Decision.ALLOW;
            }
            case WRITE -> {
                return sawUntrusted ? Decision.ASK_HUMAN : Decision.ALLOW;
            }
            default -> {
                // Private data + untrusted content + a way out is how data gets stolen.
                if (sawUntrusted && sawPrivate) return Decision.DENY;
                return sawUntrusted ? Decision.ASK_HUMAN : Decision.ALLOW;
            }
        }
    }
}

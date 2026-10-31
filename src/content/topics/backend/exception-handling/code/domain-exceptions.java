// A small hierarchy of business exceptions, each carrying what the client needs to know
public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ConflictException, BusinessRuleException {
    // stable, documented: "order_not_found"
    private final String code;
    protected DomainException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}

public final class NotFoundException extends DomainException {
    public NotFoundException(String resource, String id) {
        super(resource + "_not_found", resource + " " + id + " does not exist");
    }
}

public final class ConflictException extends DomainException {
    public ConflictException(String code, String message) { super(code, message); }
}

public final class BusinessRuleException extends DomainException {
    private final Map<String, Object> details;
    public BusinessRuleException(String code, String message, Map<String, Object> details) {
        super(code, message);
        this.details = Map.copyOf(details);
    }
    public Map<String, Object> details() { return details; }
}

// Services throw meaningful exceptions and know nothing about HTTP:
throw new BusinessRuleException("insufficient_funds", "Balance is too low for this payment",
                                Map.of("accountId", accountId, "shortfall", shortfall));

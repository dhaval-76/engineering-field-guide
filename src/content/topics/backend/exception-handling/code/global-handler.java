@RestControllerAdvice
// handles Spring MVC's own exceptions as ProblemDetail
class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.code(), ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.code(), ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail rule(BusinessRuleException ex) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage());
        ex.details().forEach(pd::setProperty);
        return pd;
    }

    // @Valid failures on request bodies → 400 with every field error (a field can have
    // several)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation_failed",
                "The request has invalid fields");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
            .toList());
        return ResponseEntity.badRequest().body(pd);
    }

    // Anything unexpected: log everything, reveal nothing
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        // trace id comes from MDC in the log pattern
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                       "Something went wrong. Quote the trace id when contacting support.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://api.example.com/problems/" + code));
        pd.setProperty("code", code);
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }
}
// application.yml: spring.mvc.problemdetails.enabled: true   (framework errors such as
// 405/415 as problem+json)

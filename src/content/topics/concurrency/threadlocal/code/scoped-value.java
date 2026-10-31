// Java 25 (final, JEP 506): ScopedValue — immutable, bound for the duration of a call,
// cleared automatically.
private static final ScopedValue<RequestContext> CTX = ScopedValue.newInstance();

void handle(HttpRequest req) {
    ScopedValue.where(CTX, RequestContext.from(req))
               // CTX.get() works anywhere inside this call tree
               .run(() -> service.process(req));
// …and is unbound again here: nothing to remove()
}

// Cheap with millions of virtual threads, and inherited by StructuredTaskScope subtasks.

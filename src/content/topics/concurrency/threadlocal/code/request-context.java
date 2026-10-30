public final class RequestContext {
    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();

    private final String userId, traceId, tenantId;

    private RequestContext(String userId, String traceId, String tenantId) {
        this.userId = userId; this.traceId = traceId; this.tenantId = tenantId;
    }

    public static void open(String userId, String traceId, String tenantId) {
        CURRENT.set(new RequestContext(userId, traceId, tenantId));
    }
    public static RequestContext current() {
        RequestContext ctx = CURRENT.get();
        if (ctx == null) throw new IllegalStateException("no request context on this thread");
        return ctx;
    }
    public static void close() { CURRENT.remove(); }      // the line that prevents the leak

    public String userId() { return userId; }
    public String traceId() { return traceId; }
    public String tenantId() { return tenantId; }
}

// A servlet filter opens the context per request and ALWAYS clears it.
@Component
public class RequestContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        RequestContext.open(req.getHeader("X-User-Id"), req.getHeader("X-Trace-Id"), req.getHeader("X-Tenant-Id"));
        try {
            chain.doFilter(req, res);
        } finally {
            RequestContext.close();                          // pool threads are reused for the next request
        }
    }
}

// Any layer, no parameter threading:
log.info("[{}] refund issued", RequestContext.current().traceId());
